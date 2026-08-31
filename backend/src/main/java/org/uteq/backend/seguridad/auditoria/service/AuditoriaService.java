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
