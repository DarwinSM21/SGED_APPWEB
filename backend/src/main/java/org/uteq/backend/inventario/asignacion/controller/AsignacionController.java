package org.uteq.backend.inventario.asignacion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.inventario.asignacion.dto.AsignacionDtos.*;
import org.uteq.backend.inventario.asignacion.service.AsignacionService;

/**
 * Asignación y devolución de artículos de inventario a estudiantes o
 * entrenadores. Los tres roles operativos pueden crear y devolver (un
 * entrenador puede entregar implementos a su equipo); ninguno gestiona
 * aquí el catálogo ni los movimientos de stock genéricos.
 */
@RestController
@RequestMapping("/api/inventario/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {
    private final AsignacionService asignacionService;

    /**
     * Lista paginada de asignaciones, de la más reciente a la más antigua.
     *
     * @param pageable paginación
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AsignacionResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(asignacionService.listarPaginado(pageable));
    }

    /**
     * Asignaciones de un estudiante.
     *
     * @param idEstudiante identificador del estudiante
     * @param pageable     paginación
     * @return {@code 200 OK} con la página
     */
    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AsignacionResponse>> listarPorEstudiante(@PathVariable Long idEstudiante, Pageable pageable) {
        return ResponseEntity.ok(asignacionService.listarPorEstudiante(idEstudiante, pageable));
    }

    /**
     * Asignaciones de un entrenador.
     *
     * @param idEntrenador identificador del entrenador
     * @param pageable     paginación
     * @return {@code 200 OK} con la página
     */
    @GetMapping("/entrenador/{idEntrenador}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AsignacionResponse>> listarPorEntrenador(@PathVariable Long idEntrenador, Pageable pageable) {
        return ResponseEntity.ok(asignacionService.listarPorEntrenador(idEntrenador, pageable));
    }

    /**
     * Registra una asignación, descontando la cantidad del stock del
     * artículo.
     *
     * @param request artículo, cantidad, destinatario y fecha esperada de
     *                devolución; validado con {@code @Valid}
     * @return {@code 201 Created} con la asignación creada
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el artículo o el destinatario no existen ({@code 404})
     * @throws IllegalArgumentException si el destinatario está mal
     *         especificado o no hay stock suficiente ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<AsignacionResponse> crear(@Valid @RequestBody AsignacionRequest request) {
        var asignacion = asignacionService.crear(request, usernameAutenticado());
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacion);
    }

    /**
     * Resuelve una asignación como {@code DEVUELTO} (repone stock) o
     * {@code PERDIDO} (no repone).
     *
     * @param id      identificador de la asignación
     * @param request estado de la devolución y observaciones; validado con
     *                {@code @Valid}
     * @return {@code 200 OK} con la asignación actualizada
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la asignación no existe ({@code 404})
     * @throws IllegalArgumentException si el estado es inválido o la
     *         asignación ya estaba resuelta ({@code 422})
     */
    @PatchMapping("/{id}/devolver")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<AsignacionResponse> devolver(@PathVariable Long id, @Valid @RequestBody DevolucionRequest request) {
        return ResponseEntity.ok(asignacionService.devolver(id, request));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
