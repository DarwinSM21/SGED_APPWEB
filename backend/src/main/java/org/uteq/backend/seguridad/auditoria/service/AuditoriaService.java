package org.uteq.backend.seguridad.auditoria.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.uteq.backend.seguridad.auditoria.dto.AuditoriaResponse;
import org.uteq.backend.seguridad.auditoria.entity.Auditoria;
import org.uteq.backend.seguridad.auditoria.repository.AuditoriaRepository;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registra filas en seguridad.auditoria. A diferencia del resto de
 * servicios del proyecto (que reciben el username ya resuelto desde el
 * controller, ver comentario en InformeService), este lee
 * SecurityContextHolder directamente: es el unico punto que se invoca
 * desde un aspecto AOP envolviendo metodos arbitrarios, que no puede
 * agregarles un parametro de username.
 *
 * <p>Nunca debe romper la operacion de negocio que audita: cualquier
 * fallo al resolver el contexto o guardar la fila se loguea y se ignora.
 * Por eso mismo los metodos de escritura usan REQUIRES_NEW -- login()
 * corre en una transaccion @Transactional(readOnly = true), y sin una
 * transaccion propia el INSERT de auditoria fallaria con "cannot execute
 * INSERT in a read-only transaction" y de paso dejaria abortada la
 * transaccion de login entero.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String entidad, Long entidadId, String descripcion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "desconocido";
        String rol = (auth != null && !auth.getAuthorities().isEmpty())
                ? auth.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "")
                : null;
        registrarConIdentidad(username, rol, accion, entidad, entidadId, descripcion);
    }

    /**
     * Variante para llamadores que ya conocen la identidad (ej.
     * AuthController en login/logout, donde SecurityContextHolder todavia
     * no tiene autenticacion resuelta porque la sesion es stateless por
     * JWT y ese token recien se esta emitiendo).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarConIdentidad(String username, String rol, String accion, String entidad,
                                       Long entidadId, String descripcion) {
        try {
            Auditoria.AuditoriaBuilder builder = Auditoria.builder()
                    .fecha(OffsetDateTime.now())
                    .usuarioNombre(username)
                    .rol(rol)
                    .accion(accion)
                    .entidad(entidad)
                    .entidadId(entidadId)
                    .descripcion(descripcion)
                    .ip(resolverIp());

            usuarioRepository.findByUsername(username).ifPresent(builder::usuario);

            auditoriaRepository.save(builder.build());
        } catch (Exception e) {
            log.error("No se pudo registrar la auditoria: accion={} entidad={} entidadId={}",
                    accion, entidad, entidadId, e);
        }
    }

    /**
     * Filtros con Specification (no JPQL "@Query" con "IS NULL OR"): con
     * parametros null combinados en un OR, PostgreSQL no siempre puede
     * inferir el tipo del parametro preparado y el driver JDBC responde
     * "could not determine data type of parameter" -- con Specification
     * cada predicado se agrega solo si el filtro esta presente, sin ese
     * problema.
     */
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> buscar(String usuario, String accion, String entidad,
                                           OffsetDateTime fechaDesde, OffsetDateTime fechaHasta,
                                           Pageable pageable) {
        Specification<Auditoria> filtro = construirFiltro(usuario, accion, entidad, fechaDesde, fechaHasta);
        return auditoriaRepository.findAll(filtro, pageable)
                .map(a -> new AuditoriaResponse(
                        a.getIdAuditoria(),
                        a.getFecha(),
                        a.getUsuarioNombre(),
                        a.getRol(),
                        a.getAccion(),
                        a.getEntidad(),
                        a.getEntidadId(),
                        a.getDescripcion()));
    }

    private Specification<Auditoria> construirFiltro(String usuario, String accion, String entidad,
                                                       OffsetDateTime fechaDesde, OffsetDateTime fechaHasta) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (usuario != null && !usuario.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("usuarioNombre")), "%" + usuario.toLowerCase() + "%"));
            }
            if (accion != null && !accion.isBlank()) {
                predicados.add(cb.equal(root.get("accion"), accion));
            }
            if (entidad != null && !entidad.isBlank()) {
                predicados.add(cb.equal(root.get("entidad"), entidad));
            }
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private String resolverIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
