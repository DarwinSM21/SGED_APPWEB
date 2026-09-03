package org.uteq.backend.deportivo.horario.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.horario.dto.HorarioRequest;
import org.uteq.backend.deportivo.horario.dto.HorarioResponse;
import org.uteq.backend.deportivo.horario.service.HorarioService;

import java.util.List;

/**
 * Horario fijo semanal del entrenador. Distinto de {@code /api/sesiones}:
 * aquí se define el patrón recurrente ("SUB-12, lunes y miércoles,
 * 16:00–18:00"); las filas concretas de {@code sesiones_entrenamiento} se
 * generan solas cada día que corresponde. El entrenador siempre opera sobre
 * sus propios horarios: la identidad sale del contexto de seguridad.
 */
@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
public class HorarioController {
    private final HorarioService horarioService;

    /**
     * Crea un horario fijo del entrenador autenticado.
     *
     * @param request categoría, día, franja horaria y campo; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el horario creado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la cuenta no tiene entrenador asociado o la categoría no
     *         existe ({@code 404})
     * @throws IllegalArgumentException si la franja es inválida o se cruza
     *         con otro horario suyo el mismo día ({@code 422})
     */
    @PostMapping
    public ResponseEntity<HorarioResponse> crear(@Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.crear(usernameAutenticado(), request));
    }

    /**
     * Horarios activos del entrenador autenticado, con aviso de cruce si lo
     * hay.
     *
     * @return {@code 200 OK} con la lista de horarios
     */
    @GetMapping("/mios")
    public ResponseEntity<List<HorarioResponse>> mios() {
        return ResponseEntity.ok(horarioService.misHorarios(usernameAutenticado()));
    }

    /**
     * Edita un horario fijo del entrenador autenticado y rehace las sesiones
     * futuras sin asistencia ni evaluación.
     *
     * @param idHorario identificador del horario
     * @param request   datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con el horario actualizado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el horario no existe o no es suyo ({@code 404})
     * @throws IllegalArgumentException si la franja es inválida o se cruza
     *         con otro horario suyo ({@code 422})
     */
    @PutMapping("/{idHorario}")
    public ResponseEntity<HorarioResponse> editar(@PathVariable Long idHorario,
                                                  @Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.ok(horarioService.editar(usernameAutenticado(), idHorario, request));
    }

    /**
     * Desactiva un horario fijo del entrenador autenticado.
     *
     * @param idHorario identificador del horario
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el horario no existe o no es suyo ({@code 404})
     */
    @DeleteMapping("/{idHorario}")
    public ResponseEntity<Void> desactivar(@PathVariable Long idHorario) {
        horarioService.desactivar(usernameAutenticado(), idHorario);
        return ResponseEntity.noContent().build();
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
