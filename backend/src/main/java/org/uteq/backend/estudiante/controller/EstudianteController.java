package org.uteq.backend.estudiante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.estudiante.dto.EstudianteRequest;
import org.uteq.backend.estudiante.dto.EstudianteResponse;
import org.uteq.backend.estudiante.service.EstudianteService;

@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ENTRENADOR','USER')")
    public ResponseEntity<Page<EstudianteResponse>> listar(
            @PageableDefault(size = 10, sort = "idEstudiante") Pageable pageable) {
        return ResponseEntity.ok(estudianteService.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ENTRENADOR','USER')")
    public ResponseEntity<EstudianteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ENTRENADOR')")
    public ResponseEntity<EstudianteResponse> crear(
            @RequestBody @Valid EstudianteRequest request) {
        return ResponseEntity.status(201).body(estudianteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ENTRENADOR')")
    public ResponseEntity<EstudianteResponse> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid EstudianteRequest request) {
        return ResponseEntity.ok(estudianteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}