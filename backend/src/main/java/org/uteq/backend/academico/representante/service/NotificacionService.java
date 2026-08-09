package org.uteq.backend.academico.representante.service;

import lombok.RequiredArgsConstructor;
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

/**
 * RF-22: notifica a los representantes de un estudiante cuando marca
 * asistencia o se le registra una lesion. Notificacion EN-APP unicamente
 * (fila en {@code academico.notificaciones}, sin correo/SMS): este
 * proyecto no tiene infraestructura de envio externo, y agregarla
 * necesitaria credenciales que nadie tiene todavia.
 *
 * <p>Si el estudiante no tiene ningun representante vinculado, no pasa
 * nada -no es un error, simplemente no hay a quien avisar-. Este efecto
 * nunca debe poder tumbar el flujo principal (marcar asistencia / registrar
 * lesion): si algun dia se le agrega logica que pueda fallar, debe fallar
 * en silencio para esa parte, no propagar la excepcion hacia arriba.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final RepresentanteEstudianteRepository vinculoRepository;
    private final RepresentanteRepository representanteRepository;

    @Transactional
    public void notificarAsistencia(Estudiante estudiante, String estadoAsistencia) {
        String estado = "TARDE".equals(estadoAsistencia) ? "con tardanza" : "a tiempo";
        crearParaCadaRepresentante(estudiante, Tipo.ASISTENCIA,
                nombreCompleto(estudiante) + " marcó asistencia hoy (" + estado + ").");
    }

    @Transactional
    public void notificarLesion(Estudiante estudiante, String descripcionLesion) {
        crearParaCadaRepresentante(estudiante, Tipo.LESION,
                "Se registró una lesión para " + nombreCompleto(estudiante) + ": " + descripcionLesion);
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

    /** 404 uniforme si la notificacion no existe o no es suya: mismo criterio IDOR del resto del modulo. */
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
