package org.uteq.backend.estudiante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.estudiante.dto.EstudianteRequest;
import org.uteq.backend.estudiante.dto.EstudianteResponse;
import org.uteq.backend.estudiante.dto.PageResponse;
import org.uteq.backend.estudiante.service.EstudianteService;

import java.util.Map;

@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ENTRENADOR','USER')")
    public ResponseEntity<PageResponse<EstudianteResponse>> listar(
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
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ENTRENADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Reporte agregado resuelto en el motor (fn SQL vía @Procedure, Bloque A.2). */
    @GetMapping("/reportes/conteo-por-categoria")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ENTRENADOR')")
    public ResponseEntity<Map<String, Object>> conteoPorCategoria(
            @RequestParam String categoria) {
        long total = estudianteService.contarActivosPorCategoria(categoria);
        return ResponseEntity.ok(Map.of("categoria", categoria, "activos", total));
    }

    /** Baja lógica masiva por categoría (procedimiento almacenado, Bloque A.2). */
    @PostMapping("/operaciones/desactivar-categoria")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> desactivarCategoria(
            @RequestParam String categoria) {
        int afectados = estudianteService.desactivarPorCategoria(categoria);
        return ResponseEntity.ok(Map.of("categoria", categoria, "desactivados", afectados));
    }
}
