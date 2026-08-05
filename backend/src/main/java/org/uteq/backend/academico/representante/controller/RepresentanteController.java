package org.uteq.backend.academico.representante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.representante.dto.RepresentantePageResponse;
import org.uteq.backend.academico.representante.dto.RepresentanteRequest;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse;
import org.uteq.backend.academico.representante.service.RepresentanteService;

/**
 * CRUD de Representante. A diferencia de {@code EntrenadorController}, no
 * se comparte lectura con ENTRENADOR: un coach no tiene motivo operativo
 * para ver datos de contacto de tutores. El alta la hace un
 * administrador sobre un idPersona/idUsuario ya creados via
 * {@code POST /api/auth/registro} (mismo patron de dos pasos que
 * Entrenador), opcionalmente vinculando de una vez a sus representados.
 */
@RestController
@RequestMapping("/api/representantes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RepresentanteController {

    private final RepresentanteService representanteService;

    @GetMapping
    public ResponseEntity<RepresentantePageResponse<RepresentanteResponse>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(representanteService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepresentanteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(representanteService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<RepresentanteResponse> crear(@Valid @RequestBody RepresentanteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(representanteService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepresentanteResponse> editar(
            @PathVariable Long id, @Valid @RequestBody RepresentanteRequest request) {
        return ResponseEntity.ok(representanteService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        representanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/estudiantes/{idEstudiante}")
    public ResponseEntity<RepresentanteResponse> vincularEstudiante(
            @PathVariable Long id, @PathVariable Long idEstudiante) {
        return ResponseEntity.ok(representanteService.vincularEstudiante(id, idEstudiante));
    }

    @DeleteMapping("/{id}/estudiantes/{idEstudiante}")
    public ResponseEntity<Void> desvincularEstudiante(
            @PathVariable Long id, @PathVariable Long idEstudiante) {
        representanteService.desvincularEstudiante(id, idEstudiante);
        return ResponseEntity.noContent().build();
    }
}
