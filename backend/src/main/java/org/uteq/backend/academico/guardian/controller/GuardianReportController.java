package org.uteq.backend.academico.guardian.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.guardian.dto.ReportDtos.*;
import org.uteq.backend.academico.guardian.dto.NotificationDtos.*;
import org.uteq.backend.academico.guardian.service.StudentReportService;
import org.uteq.backend.academico.guardian.service.NotificationService;

import java.util.List;

/**
 * Lo que ve un representante autenticado de sus propios representados. La
 * identidad sale siempre del contexto de seguridad, nunca de un parámetro
 * que el cliente pudiera manipular para ver a un estudiante ajeno.
 *
 * <p>{@code @Transactional(readOnly = true)} va también aquí, no solo en el
 * servicio: open-in-view está deshabilitado y la respuesta navega relaciones
 * LAZY ({@code Person}, {@code Category}) al construir el DTO; sin
 * transacción activa en ese momento la navegación lanza
 * {@code LazyInitializationException}.
 */
@RestController
@RequestMapping("/api/representante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('REPRESENTANTE')")
public class GuardianReportController {
    private final StudentReportService informeService;
    private final NotificationService notificacionService;

    /**
     * Lista de estudiantes que el representante autenticado tiene a cargo.
     *
     * @return {@code 200 OK} con el resumen de cada representado
     */
    @GetMapping("/estudiantes")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StudentSummaryResponse>> myStudents() {
        return ResponseEntity.ok(informeService.myStudents(authenticatedUsername()));
    }

    /**
     * Informe de evaluación de un representado.
     *
     * @param idEstudiante identificador del estudiante
     * @return {@code 200 OK} con el informe
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante no está vinculado al representante ({@code 404})
     */
    @GetMapping("/estudiantes/{idEstudiante}/informe")
    @Transactional(readOnly = true)
    public ResponseEntity<StudentReportResponse> report(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(informeService.reportFor(authenticatedUsername(), idEstudiante));
    }

    /**
     * Comentario en lenguaje natural sobre el informe de un representado. Va
     * en {@code POST} porque cada llamada consume cuota de un servicio
     * externo: se pide a demanda, no al abrir la pantalla.
     *
     * @param idEstudiante identificador del estudiante
     * @return {@code 200 OK} con el comentario generado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante no está vinculado al representante ({@code 404})
     */
    @PostMapping("/estudiantes/{idEstudiante}/informe/comentario")
    @Transactional(readOnly = true)
    public ResponseEntity<ReportCommentResponse> comment(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(informeService.commentFor(authenticatedUsername(), idEstudiante));
    }

    /**
     * Notificaciones en-app del representante (RF-22), más recientes primero.
     *
     * @return {@code 200 OK} con la lista de notificaciones
     */
    @GetMapping("/notificaciones")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NotificationResponse>> myNotifications() {
        return ResponseEntity.ok(notificacionService.myNotifications(authenticatedUsername()));
    }

    /**
     * Número de notificaciones sin leer del representante.
     *
     * @return {@code 200 OK} con el conteo
     */
    @GetMapping("/notificaciones/no-leidas")
    @Transactional(readOnly = true)
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(new UnreadCountResponse(notificacionService.unreadCount(authenticatedUsername())));
    }

    /**
     * Marca una notificación como leída.
     *
     * @param idNotificacion identificador de la notificación
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la notificación no existe o no es del representante
     *         ({@code 404})
     */
    @PostMapping("/notificaciones/{idNotificacion}/leida")
    @Transactional
    public ResponseEntity<Void> markRead(@PathVariable Long idNotificacion) {
        notificacionService.markRead(authenticatedUsername(), idNotificacion);
        return ResponseEntity.noContent().build();
    }

    private String authenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
