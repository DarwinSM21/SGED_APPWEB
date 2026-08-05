package org.uteq.backend.academico.estudianteRepresentante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.estudianteRepresentante.dto.EstudianteRepresentanteRequest;
import org.uteq.backend.academico.estudianteRepresentante.dto.EstudianteRepresentanteResponse;
import org.uteq.backend.academico.estudianteRepresentante.service.EstudianteRepresentanteService;

import java.util.List;

@RestController
@RequestMapping("/api/estudiante-representante")
@RequiredArgsConstructor
public class EstudianteRepresentanteController {

    private final EstudianteRepresentanteService estudianteRepresentanteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<Page<EstudianteRepresentanteResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(estudianteRepresentanteService.listar(pageable));
    }

    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<List<EstudianteRepresentanteResponse>> listarPorEstudiante(
            @PathVariable Long idEstudiante) {
        return ResponseEntity.ok(estudianteRepresentanteService.listarPorEstudiante(idEstudiante));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<EstudianteRepresentanteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteRepresentanteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EstudianteRepresentanteResponse> crear(
            @Valid @RequestBody EstudianteRepresentanteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estudianteRepresentanteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EstudianteRepresentanteResponse> editar(
            @PathVariable Long id, @Valid @RequestBody EstudianteRepresentanteRequest request) {
        return ResponseEntity.ok(estudianteRepresentanteService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estudianteRepresentanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
