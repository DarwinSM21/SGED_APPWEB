package org.uteq.backend.deportivo.evaluation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.evaluation.dto.EvaluationDtos.*;
import org.uteq.backend.deportivo.evaluation.service.DailyEvaluationService;

/**
 * Evaluación diaria de una sesión de entrenamiento. La alineación de partido
 * no vive aquí: se movió a {@code /api/partidos}. Todos los endpoints exigen
 * rol {@code ENTRENADOR} o {@code ADMINISTRADOR}.
 */
@RestController
@RequestMapping("/api/evaluaciones")
@RequiredArgsConstructor
public class DailyEvaluationController {
    private final DailyEvaluationService evaluacionService;

    /**
     * Abre la pantalla de evaluación de una sesión, con precarga y bloqueos.
     *
     * @param idSesion identificador de la sesión
     * @return {@code 200 OK} con criterios, jugadores evaluables y estado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la sesión no existe ({@code 404})
     */
    @GetMapping("/sesion/{idSesion}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<SessionEvaluationResponse> open(@PathVariable Long idSesion) {
        return ResponseEntity.ok(evaluacionService.open(idSesion));
    }

    /**
     * Autoguardado de un jugador. La interfaz lo llama cada vez que el
     * entrenador suelta un slider: se invoca con frecuencia y es idempotente
     * (reescribe en vez de acumular).
     *
     * @param idSesion identificador de la sesión
     * @param request  posición jugada y puntajes por criterio; validado con
     *                 {@code @Valid}
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la sesión no tiene evaluación abierta o la posición no
     *         existe ({@code 404})
     * @throws IllegalArgumentException si la evaluación ya fue finalizada, el
     *         estudiante no tiene asistencia habilitante, o un puntaje supera
     *         su máximo ({@code 422})
     */
    @PutMapping("/sesion/{idSesion}/jugadores")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Void> savePlayer(@PathVariable Long idSesion,
                                               @Valid @RequestBody SavePlayerRequest request) {
        evaluacionService.savePlayer(idSesion, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cierra la evaluación. A partir de aquí no admite cambios.
     *
     * @param idSesion           identificador de la sesión
     * @param observacionGeneral observación general de la sesión (opcional)
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la sesión no tiene evaluación abierta ({@code 404})
     * @throws IllegalArgumentException si la evaluación ya estaba finalizada
     *         ({@code 422})
     */
    @PostMapping("/sesion/{idSesion}/finalizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Void> finish(@PathVariable Long idSesion,
                                          @RequestBody(required = false) String observacionGeneral) {
        evaluacionService.finish(idSesion, observacionGeneral);
        return ResponseEntity.noContent().build();
    }
}
