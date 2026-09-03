package org.uteq.backend.deportivo.especialidad.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadRequest;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadResponse;
import org.uteq.backend.deportivo.especialidad.service.EspecialidadService;

import java.util.List;

/**
 * CRUD del catálogo de especialidades de entrenador. La lectura de activas
 * la necesita el formulario de alta/edición de entrenador
 * ({@code ADMINISTRADOR}, {@code ENTRENADOR}, {@code RECEPCIONISTA}); la
 * escritura altera un catálogo del que depende {@code Entrenador} por clave
 * foránea y queda restringida a {@code ADMINISTRADOR}.
 */
@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {
    private final EspecialidadService especialidadService;

    /**
     * Lista paginada de especialidades activas.
     *
     * @param pageable paginación y orden
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<EspecialidadResponse>> listarPaginado(Pageable pageable) {
        return ResponseEntity.ok(especialidadService.listarPaginado(pageable));
    }

    /**
     * Lista completa de especialidades activas, sin paginar (para
     * desplegables).
     *
     * @return {@code 200 OK} con todas las especialidades activas
     */
    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<EspecialidadResponse>> listarActivas() {
        return ResponseEntity.ok(especialidadService.listarTodasActivas());
    }

    /**
     * Busca una especialidad por su identificador.
     *
     * @param id identificador de la especialidad
     * @return {@code 200 OK} con la especialidad
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EspecialidadResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(especialidadService.buscarPorId(id));
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
    public ResponseEntity<EspecialidadResponse> crear(@Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crear(request));
    }

    /**
     * Actualiza el nombre de una especialidad.
     *
     * @param id      identificador de la especialidad a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con la especialidad actualizada
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si el nombre pertenece a otra
     *                                  especialidad ({@code 422})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EspecialidadResponse> editar(@PathVariable Long id, @Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.ok(especialidadService.editar(id, request));
    }

    /**
     * Baja lógica de una especialidad.
     *
     * @param id identificador de la especialidad
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        especialidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
