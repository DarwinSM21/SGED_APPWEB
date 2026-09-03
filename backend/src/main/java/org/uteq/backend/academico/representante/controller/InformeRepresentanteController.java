package org.uteq.backend.academico.representante.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.representante.dto.InformeDtos.*;
import org.uteq.backend.academico.representante.dto.NotificacionDtos.*;
import org.uteq.backend.academico.representante.service.InformeService;
import org.uteq.backend.academico.representante.service.NotificacionService;

import java.util.List;

/**
 * Lo que ve un representante autenticado de sus propios representados. La
 * identidad sale siempre del contexto de seguridad, nunca de un parámetro
 * que el cliente pudiera manipular para ver a un estudiante ajeno.
 *
 * <p>{@code @Transactional(readOnly = true)} va también aquí, no solo en el
 * servicio: open-in-view está deshabilitado y la respuesta navega relaciones
 * LAZY ({@code Persona}, {@code Categoria}) al construir el DTO; sin
 * transacción activa en ese momento la navegación lanza
 * {@code LazyInitializationException}.
 */
@RestController
@RequestMapping("/api/representante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('REPRESENTANTE')")
public class InformeRepresentanteController {
    private final InformeService informeService;
    private final NotificacionService notificacionService;

    /**
     * Lista de estudiantes que el representante autenticado tiene a cargo.
     *
     * @return {@code 200 OK} con el resumen de cada representado
     */
    @GetMapping("/estudiantes")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EstudianteResumenResponse>> misRepresentados() {
        return ResponseEntity.ok(informeService.misRepresentados(usernameAutenticado()));
    }

    /**
     * Informe de evaluación de un representado.
     *
     * @param idEstudiante identificador del estudiante
     * @return {@code 200 OK} con el informe
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante no está vinculado al representante ({@code 404})
     */
    @GetMapping("/estudiantes/{idEstudiante}/informe")
    @Transactional(readOnly = true)
    public ResponseEntity<InformeEstudianteResponse> informe(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(informeService.informeDe(usernameAutenticado(), idEstudiante));
    }

    /**
     * Comentario en lenguaje natural sobre el informe de un representado. Va
     * en {@code POST} porque cada llamada consume cuota de un servicio
     * externo: se pide a demanda, no al abrir la pantalla.
     *
     * @param idEstudiante identificador del estudiante
     * @return {@code 200 OK} con el comentario generado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante no está vinculado al representante ({@code 404})
     */
    @PostMapping("/estudiantes/{idEstudiante}/informe/comentario")
    @Transactional(readOnly = true)
    public ResponseEntity<ComentarioInformeResponse> comentario(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(informeService.comentarioDe(usernameAutenticado(), idEstudiante));
    }

    /**
     * Notificaciones en-app del representante (RF-22), más recientes primero.
     *
     * @return {@code 200 OK} con la lista de notificaciones
     */
    @GetMapping("/notificaciones")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NotificacionResponse>> misNotificaciones() {
        return ResponseEntity.ok(notificacionService.misNotificaciones(usernameAutenticado()));
    }

    /**
     * Número de notificaciones sin leer del representante.
     *
     * @return {@code 200 OK} con el conteo
     */
    @GetMapping("/notificaciones/no-leidas")
    @Transactional(readOnly = true)
    public ResponseEntity<ConteoNoLeidasResponse> conteoNoLeidas() {
        return ResponseEntity.ok(new ConteoNoLeidasResponse(notificacionService.conteoNoLeidas(usernameAutenticado())));
    }

    /**
     * Marca una notificación como leída.
     *
     * @param idNotificacion identificador de la notificación
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la notificación no existe o no es del representante
     *         ({@code 404})
     */
    @PostMapping("/notificaciones/{idNotificacion}/leida")
    @Transactional
    public ResponseEntity<Void> marcarLeida(@PathVariable Long idNotificacion) {
        notificacionService.marcarLeida(usernameAutenticado(), idNotificacion);
        return ResponseEntity.noContent().build();
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
