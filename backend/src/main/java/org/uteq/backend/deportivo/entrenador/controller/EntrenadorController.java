package org.uteq.backend.deportivo.entrenador.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorPageResponse;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorRequest;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorResponse;
import org.uteq.backend.deportivo.entrenador.service.EntrenadorService;

@RestController
@RequestMapping("/api/entrenadores")
@RequiredArgsConstructor
public class EntrenadorController {
    private final EntrenadorService entrenadorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EntrenadorPageResponse<EntrenadorResponse>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(entrenadorService.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EntrenadorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(entrenadorService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EntrenadorResponse> crear(@Valid @RequestBody EntrenadorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entrenadorService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EntrenadorResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody EntrenadorRequest request) {
        return ResponseEntity.ok(entrenadorService.editar(id, request));
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EntrenadorResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(entrenadorService.reactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        entrenadorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
