package org.uteq.backend.estudiante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.estudiante.dto.EstudianteRequest;
import org.uteq.backend.estudiante.dto.EstudianteResponse;
import org.uteq.backend.estudiante.dto.PageResponse;
import org.uteq.backend.estudiante.service.EstudianteService;

/**
 * CRUD completo de Estudiante con paginacion y soft delete.
 * Endpoints sensibles requieren rol ADMINISTRADOR via @PreAuthorize.
 */
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<PageResponse<EstudianteResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idEstudiante,asc") String[] sort) {
        String campo = sort[0];
        Sort.Direction dir = sort.length > 1 && "desc".equalsIgnoreCase(sort[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(dir, campo));
        return ResponseEntity.ok(estudianteService.listar(pageRequest));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<EstudianteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EstudianteResponse> crear(
            @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estudianteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EstudianteResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.ok(estudianteService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conteo/{categoria}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Long> contarActivos(@PathVariable String categoria) {
        return ResponseEntity.ok(estudianteService.contarActivosPorCategoria(categoria));
    }
}
