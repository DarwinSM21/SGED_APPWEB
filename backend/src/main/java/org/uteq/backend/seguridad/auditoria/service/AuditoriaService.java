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
 * Registra filas en {@code seguridad.auditoria}. A diferencia del resto de
 * servicios (que reciben el username ya resuelto desde el controlador), este
 * lee {@code SecurityContextHolder} directamente: es el único punto invocado
 * desde un aspecto AOP que envuelve métodos arbitrarios y no puede añadirles
 * un parámetro de username.
 *
 * <p>Nunca debe romper la operación de negocio que audita: cualquier fallo
 * al resolver el contexto o guardar la fila se registra y se ignora. Por eso
 * los métodos de escritura usan {@code REQUIRES_NEW} — {@code login()} corre
 * en una transacción de solo lectura, y sin transacción propia el
 * {@code INSERT} fallaría y abortaría la del login entero.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra un evento resolviendo la identidad del actor desde el contexto
     * de seguridad actual.
     *
     * @param accion      acción realizada, p. ej. {@code "CREAR"}
     * @param entidad     nombre de la entidad afectada
     * @param entidadId   identificador de la fila afectada; puede ser
     *                    {@code null}
     * @param descripcion texto legible del evento
     */
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
     * Variante para llamadores que ya conocen la identidad (p. ej.
     * {@code AuthService} en login/logout, donde el contexto de seguridad
     * todavía no tiene autenticación resuelta porque la sesión es stateless
     * por JWT y ese token recién se está emitiendo).
     *
     * @param username    nombre de usuario del actor
     * @param rol         rol del actor; puede ser {@code null}
     * @param accion      acción realizada
     * @param entidad     nombre de la entidad afectada
     * @param entidadId   identificador de la fila afectada; puede ser
     *                    {@code null}
     * @param descripcion texto legible del evento
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
     * Busca eventos de auditoría aplicando solo los filtros presentes.
     *
     * @param usuario    subcadena del nombre de usuario; ignorado si es
     *                   {@code null} o en blanco
     * @param accion     acción exacta; ignorado si es {@code null} o en blanco
     * @param entidad    entidad exacta; ignorado si es {@code null} o en blanco
     * @param fechaDesde límite inferior de fecha; ignorado si es {@code null}
     * @param fechaHasta límite superior de fecha; ignorado si es {@code null}
     * @param pageable   paginación y orden
     * @return la página de eventos que cumplen los filtros
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

    // Filtros con Specification (no "@Query" con "IS NULL OR"): con parámetros
    // null combinados en un OR, PostgreSQL no siempre infiere el tipo del
    // parámetro preparado y el driver responde "could not determine data type
    // of parameter". Con Specification cada predicado se agrega solo si el
    // filtro está presente.
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
