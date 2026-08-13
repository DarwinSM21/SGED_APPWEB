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
import org.uteq.backend.academico.representante.dto.VinculoRequest;
import org.uteq.backend.academico.representante.service.RepresentanteService;

/**
 * CRUD de Representante. A diferencia de {@code EntrenadorController}, no
 * se comparte lectura con ENTRENADOR: un coach no tiene motivo operativo
 * para ver datos de contacto de tutores. El alta la hace un
 * administrador o un recepcionista (pantalla unificada de Personas)
 * sobre un idPersona/idUsuario ya creados via {@code POST /api/usuarios}
 * (mismo patron de dos pasos que Entrenador), opcionalmente vinculando de
 * una vez a sus representados. RECEPCIONISTA solo puede leer/crear/
 * vincular -- editar datos de un representante existente o eliminarlo
 * sigue siendo exclusivo de ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/representantes")
@RequiredArgsConstructor
public class RepresentanteController {

    private final RepresentanteService representanteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<RepresentantePageResponse<RepresentanteResponse>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(representanteService.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<RepresentanteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(representanteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<RepresentanteResponse> crear(@Valid @RequestBody RepresentanteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(representanteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RepresentanteResponse> editar(
            @PathVariable Long id, @Valid @RequestBody RepresentanteRequest request) {
        return ResponseEntity.ok(representanteService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        representanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * El cuerpo es opcional: sin el, el vinculo queda sin relacion y sin
     * contacto principal (mismo resultado que antes de exponer esos campos).
     */
    @PostMapping("/{id}/estudiantes/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<RepresentanteResponse> vincularEstudiante(
            @PathVariable Long id, @PathVariable Long idEstudiante,
            @Valid @RequestBody(required = false) VinculoRequest request) {
        return ResponseEntity.ok(representanteService.vincularEstudiante(id, idEstudiante, request));
    }

    @DeleteMapping("/{id}/estudiantes/{idEstudiante}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desvincularEstudiante(
            @PathVariable Long id, @PathVariable Long idEstudiante) {
        representanteService.desvincularEstudiante(id, idEstudiante);
        return ResponseEntity.noContent().build();
    }
}
