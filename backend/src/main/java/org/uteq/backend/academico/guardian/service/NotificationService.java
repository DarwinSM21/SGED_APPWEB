package org.uteq.backend.academico.guardian.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.guardian.dto.NotificationDtos.NotificationResponse;
import org.uteq.backend.academico.guardian.entity.Notification;
import org.uteq.backend.academico.guardian.entity.Notification.Type;
import org.uteq.backend.academico.guardian.entity.Guardian;
import org.uteq.backend.academico.guardian.repository.NotificationRepository;
import org.uteq.backend.academico.guardian.repository.GuardianStudentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;

import java.util.List;
import org.uteq.backend.academico.guardian.entity.Consent;
import org.uteq.backend.academico.guardian.repository.ConsentRepository;

/**
 * RF-22: notifica a los representantes de un estudiante cuando marca
 * asistencia o se le registra una lesión. Notificación en-app únicamente
 * (fila en {@code academico.notificaciones}, sin correo ni SMS): el proyecto
 * no tiene infraestructura de envío externo.
 *
 * <p>Si el estudiante no tiene ningún representante vinculado, no pasa nada.
 * Este efecto nunca debe poder tumbar el flujo principal (marcar asistencia,
 * registrar lesión): falla en silencio para esa parte, no propaga la
 * excepción. Cada notificación requiere además un consentimiento vigente del
 * representante con el alcance correspondiente (hallazgo H-04).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificacionRepository;
    private final GuardianStudentRepository vinculoRepository;
    private final GuardianRepository representanteRepository;
    private final ConsentRepository consentimientoRepository;

    /**
     * Crea una notificación de asistencia para cada representante autorizado
     * del estudiante. No lanza: un fallo se registra y se traga.
     *
     * @param estudiante       estudiante que marcó asistencia
     * @param estadoAsistencia estado marcado ({@code "TARDE"} o presente)
     */
    @Transactional
    public void notifyAttendance(Student estudiante, String estadoAsistencia) {
        withoutBreakingMainFlow("asistencia", () -> {
            String estado = "TARDE".equals(estadoAsistencia) ? "con tardanza" : "a tiempo";
            createForEachGuardian(estudiante, Type.ASISTENCIA,
                    Consent.ALCANCE_NOTIFICACIONES_ASISTENCIA,
                    fullName(estudiante) + " marcó asistencia hoy (" + estado + ").");
        });
    }

    /**
     * Crea una notificación de lesión para cada representante autorizado del
     * estudiante. No lanza: un fallo se registra y se traga.
     *
     * @param estudiante        estudiante lesionado
     * @param descripcionLesion descripción de la lesión registrada
     */
    @Transactional
    public void notifyInjury(Student estudiante, String descripcionLesion) {
        withoutBreakingMainFlow("lesion", () ->
                createForEachGuardian(estudiante, Type.LESION,
                        Consent.ALCANCE_NOTIFICACIONES_LESION,
                        "Se registró una lesión para " + fullName(estudiante) + ": " + descripcionLesion));
    }

    /**
     * Ejecuta el efecto de notificación sin dejar que su fallo se propague.
     *
     * <p>La captura tiene que estar <b>aquí dentro</b> y no en quien llama:
     * estos métodos son {@code @Transactional} y se invocan desde
     * {@code AsistenciaService} / {@code LesionService}, ya dentro de una
     * transacción. Si la excepción saliera, el proxy de Spring marcaría la
     * transacción como {@code rollback-only} y el {@code try/catch} del
     * llamador no serviría —al confirmar saltaría
     * {@code UnexpectedRollbackException} y se perdería la asistencia ya
     * marcada—.
     *
     * <p>Límite conocido: cubre fallos de nivel de aplicación. Un fallo de
     * nivel de base (violación de FK) aborta la transacción en PostgreSQL y
     * ya no hay captura en Java que lo rescate; aislarlo del todo exigiría
     * {@code REQUIRES_NEW} o un evento {@code AFTER_COMMIT}, anotado como
     * trabajo futuro.
     *
     * @param contexto etiqueta para el log ({@code "asistencia"} /
     *                 {@code "lesion"})
     * @param efecto   el efecto de notificación a ejecutar
     */
    private void withoutBreakingMainFlow(String contexto, Runnable efecto) {
        try {
            efecto.run();
        } catch (RuntimeException e) {
            log.warn("No se pudo notificar a los representantes ({}): {}. "
                            + "El registro principal se conserva.",
                    contexto, e.getClass().getSimpleName());
        }
    }

    /**
     * Notificaciones del representante autenticado, más recientes primero.
     *
     * @param username nombre de usuario del representante
     * @return la lista de notificaciones
     * @throws ResourceNotFoundException si la cuenta no tiene un
     *                                      representante asociado
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications(String username) {
        Guardian representante = guardianOf(username);
        return notificacionRepository
                .findByRepresentante_IdRepresentanteOrderByCreatedAtDesc(representante.getId())
                .stream().map(this::toResponse).toList();
    }

    /**
     * Número de notificaciones sin leer del representante autenticado.
     *
     * @param username nombre de usuario del representante
     * @return el conteo de no leídas
     * @throws ResourceNotFoundException si la cuenta no tiene un
     *                                      representante asociado
     */
    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        Guardian representante = guardianOf(username);
        return notificacionRepository.countByRepresentante_IdRepresentanteAndLeidaFalse(representante.getId());
    }

    /**
     * Marca una notificación del representante autenticado como leída.
     * Responde {@code 404} uniforme si no existe o no es suya (mismo criterio
     * IDOR del resto del módulo).
     *
     * @param username       nombre de usuario del representante
     * @param idNotificacion identificador de la notificación
     * @throws ResourceNotFoundException si la notificación no existe o no
     *                                      pertenece al representante
     */
    @Transactional
    public void markRead(String username, Long idNotificacion) {
        Guardian representante = guardianOf(username);
        Notification notificacion = notificacionRepository
                .findByIdNotificacionAndRepresentante_IdRepresentante(idNotificacion, representante.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada con id: " + idNotificacion));
        notificacion.setRead(true);
        notificacionRepository.save(notificacion);
    }

    private void createForEachGuardian(Student estudiante, Type tipo,
                                            String alcanceRequerido, String mensaje) {
        List<Guardian> representantes = vinculoRepository
                .findByEstudiante_IdEstudianteAndActivoTrue(estudiante.getId())
                .stream().map(v -> v.getGuardian()).toList();

        for (Guardian representante : representantes) {
            if (!isAuthorized(representante, estudiante, alcanceRequerido)) {
                log.info("No se notifica al representante {} sobre el estudiante {}: "
                                + "no hay consentimiento vigente para {}",
                        representante.getId(), estudiante.getId(), alcanceRequerido);
                continue;
            }
            notificacionRepository.save(Notification.builder()
                    .guardian(representante)
                    .student(estudiante)
                    .type(tipo)
                    .message(mensaje)
                    .read(false)
                    .build());
        }
    }

    private boolean isAuthorized(Guardian representante, Student estudiante, String alcance) {
        Long idR = representante.getId();
        Long idE = estudiante.getId();
        return isCurrent(idR, idE, alcance)
                || isCurrent(idR, idE, Consent.ALCANCE_NOTIFICACIONES);
    }

    private boolean isCurrent(Long idRepresentante, Long idEstudiante, String alcance) {
        return consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        idRepresentante, idEstudiante, alcance)
                .isPresent();
    }

    private Guardian guardianOf(String username) {
        return representanteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un representante asociado a esta cuenta"));
    }

    private String fullName(Student e) {
        var p = e.getPerson();
        return p.getName() + " " + p.getLastName();
    }

    private NotificationResponse toResponse(Notification n) {
        var persona = n.getStudent().getPerson();
        return new NotificationResponse(
                n.getId(),
                n.getStudent().getId(),
                persona.getName() + " " + persona.getLastName(),
                n.getType(),
                n.getMessage(),
                n.getRead(),
                n.getCreatedAt());
    }
}
