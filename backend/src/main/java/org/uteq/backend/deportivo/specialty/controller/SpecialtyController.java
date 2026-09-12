package org.uteq.backend.deportivo.specialty.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.specialty.dto.SpecialtyRequest;
import org.uteq.backend.deportivo.specialty.dto.SpecialtyResponse;
import org.uteq.backend.deportivo.specialty.service.SpecialtyService;

import java.util.List;

/**
 * CRUD del catálogo de especialidades de entrenador. La lectura de activas
 * la necesita el formulario de alta/edición de entrenador
 * ({@code ADMINISTRADOR}, {@code ENTRENADOR}, {@code RECEPCIONISTA}); la
 * escritura altera un catálogo del que depende {@code Coach} por clave
 * foránea y queda restringida a {@code ADMINISTRADOR}.
 */
@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class SpecialtyController {
    private final SpecialtyService specialtyService;

    /**
     * Lista paginada de especialidades activas.
     *
     * @param pageable paginación y orden
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<SpecialtyResponse>> findPaged(Pageable pageable) {
        return ResponseEntity.ok(specialtyService.findPaged(pageable));
    }

    /**
     * Lista completa de especialidades activas, sin paginar (para
     * desplegables).
     *
     * @return {@code 200 OK} con todas las especialidades activas
     */
    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<SpecialtyResponse>> findActive() {
        return ResponseEntity.ok(specialtyService.findAllActive());
    }

    /**
     * Busca una especialidad por su identificador.
     *
     * @param id identificador de la especialidad
     * @return {@code 200 OK} con la especialidad
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SpecialtyResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(specialtyService.findById(id));
    }

    /**
     * Crea una especialidad.
     *
     * @param request nombre de la especialidad; validado con {@code @Valid}
     * @return {@code 201 Created} con la especialidad creada
     * @throws IllegalArgumentException si ya existe una especialidad con ese
     *                                  nombre ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SpecialtyResponse> create(@Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specialtyService.create(request));
    }

    /**
     * Actualiza el nombre de una especialidad.
     *
     * @param id      identificador de la especialidad a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con la especialidad actualizada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si el nombre pertenece a otra
     *                                  especialidad ({@code 422})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SpecialtyResponse> update(@PathVariable Long id, @Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.ok(specialtyService.update(id, request));
    }

    /**
     * Baja lógica de una especialidad.
     *
     * @param id identificador de la especialidad
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specialtyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
