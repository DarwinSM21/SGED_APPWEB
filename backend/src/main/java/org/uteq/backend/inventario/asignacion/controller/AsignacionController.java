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

@RestController
@RequestMapping("/api/inventario/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {
    private final AsignacionService asignacionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AsignacionResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(asignacionService.listarPaginado(pageable));
    }

    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AsignacionResponse>> listarPorEstudiante(@PathVariable Long idEstudiante, Pageable pageable) {
        return ResponseEntity.ok(asignacionService.listarPorEstudiante(idEstudiante, pageable));
    }

    @GetMapping("/entrenador/{idEntrenador}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AsignacionResponse>> listarPorEntrenador(@PathVariable Long idEntrenador, Pageable pageable) {
        return ResponseEntity.ok(asignacionService.listarPorEntrenador(idEntrenador, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<AsignacionResponse> crear(@Valid @RequestBody AsignacionRequest request) {
        var asignacion = asignacionService.crear(request, usernameAutenticado());
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacion);
    }

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
