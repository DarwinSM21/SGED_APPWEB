package org.uteq.backend.academico.representante.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.representante.dto.NotificacionDtos.NotificacionResponse;
import org.uteq.backend.academico.representante.entity.Notificacion;
import org.uteq.backend.academico.representante.entity.Notificacion.Tipo;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.NotificacionRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final RepresentanteEstudianteRepository vinculoRepository;
    private final RepresentanteRepository representanteRepository;

    @Transactional
    public void notificarAsistencia(Estudiante estudiante, String estadoAsistencia) {
        sinTumbarElFlujoPrincipal("asistencia", () -> {
            String estado = "TARDE".equals(estadoAsistencia) ? "con tardanza" : "a tiempo";
            crearParaCadaRepresentante(estudiante, Tipo.ASISTENCIA,
                    nombreCompleto(estudiante) + " marcó asistencia hoy (" + estado + ").");
        });
    }

    @Transactional
    public void notificarLesion(Estudiante estudiante, String descripcionLesion) {
        sinTumbarElFlujoPrincipal("lesion", () ->
                crearParaCadaRepresentante(estudiante, Tipo.LESION,
                        "Se registró una lesión para " + nombreCompleto(estudiante) + ": " + descripcionLesion));
    }

    private void sinTumbarElFlujoPrincipal(String contexto, Runnable efecto) {
        try {
            efecto.run();
        } catch (RuntimeException e) {
            log.warn("No se pudo notificar a los representantes ({}): {}. "
                            + "El registro principal se conserva.",
                    contexto, e.getClass().getSimpleName());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> misNotificaciones(String username) {
        Representante representante = representanteDe(username);
        return notificacionRepository
                .findByRepresentante_IdRepresentanteOrderByCreatedAtDesc(representante.getIdRepresentante())
                .stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public long conteoNoLeidas(String username) {
        Representante representante = representanteDe(username);
        return notificacionRepository.countByRepresentante_IdRepresentanteAndLeidaFalse(representante.getIdRepresentante());
    }

    @Transactional
    public void marcarLeida(String username, Long idNotificacion) {
        Representante representante = representanteDe(username);
        Notificacion notificacion = notificacionRepository
                .findByIdNotificacionAndRepresentante_IdRepresentante(idNotificacion, representante.getIdRepresentante())
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada con id: " + idNotificacion));
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    private void crearParaCadaRepresentante(Estudiante estudiante, Tipo tipo, String mensaje) {
        List<Representante> representantes = vinculoRepository
                .findByEstudiante_IdEstudianteAndActivoTrue(estudiante.getIdEstudiante())
                .stream().map(v -> v.getRepresentante()).toList();

        for (Representante representante : representantes) {
            notificacionRepository.save(Notificacion.builder()
                    .representante(representante)
                    .estudiante(estudiante)
                    .tipo(tipo)
                    .mensaje(mensaje)
                    .leida(false)
                    .build());
        }
    }

    private Representante representanteDe(String username) {
        return representanteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un representante asociado a esta cuenta"));
    }

    private String nombreCompleto(Estudiante e) {
        var p = e.getPersona();
        return p.getNombre() + " " + p.getApellido();
    }

    private NotificacionResponse aResponse(Notificacion n) {
        var persona = n.getEstudiante().getPersona();
        return new NotificacionResponse(
                n.getIdNotificacion(),
                n.getEstudiante().getIdEstudiante(),
                persona.getNombre() + " " + persona.getApellido(),
                n.getTipo(),
                n.getMensaje(),
                n.getLeida(),
                n.getCreatedAt());
    }
}
