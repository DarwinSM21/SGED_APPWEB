package org.uteq.backend.deportivo.coach.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.coach.dto.CoachPageResponse;
import org.uteq.backend.deportivo.coach.dto.CoachRequest;
import org.uteq.backend.deportivo.coach.dto.CoachResponse;
import org.uteq.backend.deportivo.coach.service.CoachService;

/**
 * CRUD de {@code Coach}. El alta y la baja crean o retiran el vínculo
 * con una cuenta de usuario, por lo que son operaciones de
 * {@code ADMINISTRADOR}; la consulta la comparten {@code ADMINISTRADOR},
 * {@code ENTRENADOR} y {@code RECEPCIONISTA} (esta última necesita listar
 * entrenadores para asignarles artículos de inventario).
 */
@RestController
@RequestMapping("/api/entrenadores")
@RequiredArgsConstructor
public class CoachController {
    private final CoachService coachService;

    /**
     * Lista paginada de entrenadores.
     *
     * @param pageable paginación; por defecto 10 por página
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<CoachPageResponse<CoachResponse>> list(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(coachService.list(pageable));
    }

    /**
     * Busca un entrenador por su identificador.
     *
     * @param id identificador del entrenador
     * @return {@code 200 OK} con el entrenador
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<CoachResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(coachService.findById(id));
    }

    /**
     * Registra un entrenador sobre una persona y una cuenta ya creadas.
     *
     * @param request persona, usuario, especialidad y datos profesionales;
     *                validado con {@code @Valid}
     * @return {@code 201 Created} con el entrenador registrado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la persona, el usuario o la especialidad no existen
     *         ({@code 404})
     * @throws IllegalArgumentException si la persona o el usuario ya están
     *         asignados, o el usuario no tiene rol {@code ENTRENADOR}
     *         ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CoachResponse> create(@Valid @RequestBody CoachRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coachService.create(request));
    }

    /**
     * Actualiza la especialidad y los datos profesionales de un entrenador.
     *
     * @param id      identificador del entrenador a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con el entrenador actualizado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el entrenador o la especialidad no existen ({@code 404})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CoachResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CoachRequest request) {
        return ResponseEntity.ok(coachService.update(id, request));
    }

    /**
     * Reactiva un entrenador dado de baja.
     *
     * @param id identificador del entrenador
     * @return {@code 200 OK} con el entrenador reactivado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si ya está activo ({@code 422})
     */
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CoachResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(coachService.reactivate(id));
    }

    /**
     * Baja lógica de un entrenador.
     *
     * @param id identificador del entrenador
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        coachService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
