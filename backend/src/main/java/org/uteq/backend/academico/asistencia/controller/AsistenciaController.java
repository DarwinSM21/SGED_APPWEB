package org.uteq.backend.academico.asistencia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.asistencia.dto.AsistenciaRequest;
import org.uteq.backend.academico.asistencia.dto.AsistenciaResponse;
import org.uteq.backend.academico.asistencia.service.AsistenciaService;

import java.util.List;

/**
 * CRUD de Asistencia. El registro lo hacen ADMINISTRADOR y ENTRENADOR
 * (toman lista en sus propias sesiones); la lectura la necesitan todos
 * los roles.
 */
@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<Page<AsistenciaResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(asistenciaService.listar(pageable));
    }

    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<List<AsistenciaResponse>> listarPorEstudiante(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(asistenciaService.listarPorEstudiante(idEstudiante));
    }

    @GetMapping("/sesion/{idSesionEntrenamiento}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<List<AsistenciaResponse>> listarPorSesion(@PathVariable Long idSesionEntrenamiento) {
        return ResponseEntity.ok(asistenciaService.listarPorSesion(idSesionEntrenamiento));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<AsistenciaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(asistenciaService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<AsistenciaResponse> crear(@Valid @RequestBody AsistenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asistenciaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<AsistenciaResponse> editar(
            @PathVariable Long id, @Valid @RequestBody AsistenciaRequest request) {
        return ResponseEntity.ok(asistenciaService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        asistenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
