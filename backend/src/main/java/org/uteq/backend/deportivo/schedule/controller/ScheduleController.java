package org.uteq.backend.deportivo.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.schedule.dto.ScheduleRequest;
import org.uteq.backend.deportivo.schedule.dto.ScheduleResponse;
import org.uteq.backend.deportivo.schedule.service.ScheduleService;

import java.util.List;

/**
 * Schedule fijo semanal del entrenador. Distinto de {@code /api/sesiones}:
 * aquí se define el patrón recurrente ("SUB-12, lunes y miércoles,
 * 16:00–18:00"); las filas concretas de {@code sesiones_entrenamiento} se
 * generan solas cada día que corresponde. El entrenador siempre opera sobre
 * sus propios horarios: la identidad sale del contexto de seguridad.
 */
@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
public class ScheduleController {
    private final ScheduleService scheduleService;

    /**
     * Crea un horario fijo del entrenador autenticado.
     *
     * @param request categoría, día, franja horaria y campo; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el horario creado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la cuenta no tiene entrenador asociado o la categoría no
     *         existe ({@code 404})
     * @throws IllegalArgumentException si la franja es inválida o se cruza
     *         con otro horario suyo el mismo día ({@code 422})
     */
    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(authenticatedUsername(), request));
    }

    /**
     * Horarios activos del entrenador autenticado, con aviso de cruce si lo
     * hay.
     *
     * @return {@code 200 OK} con la lista de horarios
     */
    @GetMapping("/mios")
    public ResponseEntity<List<ScheduleResponse>> mine() {
        return ResponseEntity.ok(scheduleService.mySchedules(authenticatedUsername()));
    }

    /**
     * Edita un horario fijo del entrenador autenticado y rehace las sesiones
     * futuras sin asistencia ni evaluación.
     *
     * @param idHorario identificador del horario
     * @param request   datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con el horario actualizado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el horario no existe o no es suyo ({@code 404})
     * @throws IllegalArgumentException si la franja es inválida o se cruza
     *         con otro horario suyo ({@code 422})
     */
    @PutMapping("/{idHorario}")
    public ResponseEntity<ScheduleResponse> update(@PathVariable Long idHorario,
                                                  @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.update(authenticatedUsername(), idHorario, request));
    }

    /**
     * Desactiva un horario fijo del entrenador autenticado.
     *
     * @param idHorario identificador del horario
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el horario no existe o no es suyo ({@code 404})
     */
    @DeleteMapping("/{idHorario}")
    public ResponseEntity<Void> deactivate(@PathVariable Long idHorario) {
        scheduleService.deactivate(authenticatedUsername(), idHorario);
        return ResponseEntity.noContent().build();
    }

    private String authenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
