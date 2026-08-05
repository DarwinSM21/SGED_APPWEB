package org.uteq.backend.academico.representante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.representante.dto.RepresentanteRequest;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse;
import org.uteq.backend.academico.representante.service.RepresentanteService;

@RestController
@RequestMapping("/api/representantes")
@RequiredArgsConstructor
public class RepresentanteController {

    private final RepresentanteService representanteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<Page<RepresentanteResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(representanteService.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<RepresentanteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(representanteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
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
}
