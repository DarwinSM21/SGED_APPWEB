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
import org.uteq.backend.academico.representante.entity.Consentimiento;
import org.uteq.backend.academico.representante.repository.ConsentimientoRepository;

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
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final RepresentanteEstudianteRepository vinculoRepository;
    private final RepresentanteRepository representanteRepository;
    private final ConsentimientoRepository consentimientoRepository;

    /**
     * Crea una notificación de asistencia para cada representante autorizado
     * del estudiante. No lanza: un fallo se registra y se traga.
     *
     * @param estudiante       estudiante que marcó asistencia
     * @param estadoAsistencia estado marcado ({@code "TARDE"} o presente)
     */
    @Transactional
    public void notificarAsistencia(Estudiante estudiante, String estadoAsistencia) {
        sinTumbarElFlujoPrincipal("asistencia", () -> {
            String estado = "TARDE".equals(estadoAsistencia) ? "con tardanza" : "a tiempo";
            crearParaCadaRepresentante(estudiante, Tipo.ASISTENCIA,
                    Consentimiento.ALCANCE_NOTIFICACIONES_ASISTENCIA,
                    nombreCompleto(estudiante) + " marcó asistencia hoy (" + estado + ").");
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
    public void notificarLesion(Estudiante estudiante, String descripcionLesion) {
        sinTumbarElFlujoPrincipal("lesion", () ->
                crearParaCadaRepresentante(estudiante, Tipo.LESION,
                        Consentimiento.ALCANCE_NOTIFICACIONES_LESION,
                        "Se registró una lesión para " + nombreCompleto(estudiante) + ": " + descripcionLesion));
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
    private void sinTumbarElFlujoPrincipal(String contexto, Runnable efecto) {
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
     * @throws RecursoNoEncontradoException si la cuenta no tiene un
     *                                      representante asociado
     */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> misNotificaciones(String username) {
        Representante representante = representanteDe(username);
        return notificacionRepository
                .findByRepresentante_IdRepresentanteOrderByCreatedAtDesc(representante.getIdRepresentante())
                .stream().map(this::aResponse).toList();
    }

    /**
     * Número de notificaciones sin leer del representante autenticado.
     *
     * @param username nombre de usuario del representante
     * @return el conteo de no leídas
     * @throws RecursoNoEncontradoException si la cuenta no tiene un
     *                                      representante asociado
     */
    @Transactional(readOnly = true)
    public long conteoNoLeidas(String username) {
        Representante representante = representanteDe(username);
        return notificacionRepository.countByRepresentante_IdRepresentanteAndLeidaFalse(representante.getIdRepresentante());
    }

    /**
     * Marca una notificación del representante autenticado como leída.
     * Responde {@code 404} uniforme si no existe o no es suya (mismo criterio
     * IDOR del resto del módulo).
     *
     * @param username       nombre de usuario del representante
     * @param idNotificacion identificador de la notificación
     * @throws RecursoNoEncontradoException si la notificación no existe o no
     *                                      pertenece al representante
     */
    @Transactional
    public void marcarLeida(String username, Long idNotificacion) {
        Representante representante = representanteDe(username);
        Notificacion notificacion = notificacionRepository
                .findByIdNotificacionAndRepresentante_IdRepresentante(idNotificacion, representante.getIdRepresentante())
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada con id: " + idNotificacion));
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    private void crearParaCadaRepresentante(Estudiante estudiante, Tipo tipo,
                                            String alcanceRequerido, String mensaje) {
        List<Representante> representantes = vinculoRepository
                .findByEstudiante_IdEstudianteAndActivoTrue(estudiante.getIdEstudiante())
                .stream().map(v -> v.getRepresentante()).toList();

        for (Representante representante : representantes) {
            if (!autorizo(representante, estudiante, alcanceRequerido)) {
                log.info("No se notifica al representante {} sobre el estudiante {}: "
                                + "no hay consentimiento vigente para {}",
                        representante.getIdRepresentante(), estudiante.getIdEstudiante(), alcanceRequerido);
                continue;
            }
            notificacionRepository.save(Notificacion.builder()
                    .representante(representante)
                    .estudiante(estudiante)
                    .tipo(tipo)
                    .mensaje(mensaje)
                    .leida(false)
                    .build());
        }
    }

    private boolean autorizo(Representante representante, Estudiante estudiante, String alcance) {
        Long idR = representante.getIdRepresentante();
        Long idE = estudiante.getIdEstudiante();
        return vigente(idR, idE, alcance)
                || vigente(idR, idE, Consentimiento.ALCANCE_NOTIFICACIONES);
    }

    private boolean vigente(Long idRepresentante, Long idEstudiante, String alcance) {
        return consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        idRepresentante, idEstudiante, alcance)
                .isPresent();
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
