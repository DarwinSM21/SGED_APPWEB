package org.uteq.backend.deportivo.especialidad.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadRequest;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadResponse;
import org.uteq.backend.deportivo.especialidad.service.EspecialidadService;

import java.util.List;

/**
 * CRUD del catalogo de especialidades de Entrenador. La lectura de
 * activas la necesita el formulario de alta/edicion de entrenador
 * (ADMINISTRADOR, ENTRENADOR, RECEPCIONISTA -- mismos roles que
 * /api/categorias/activas); la escritura altera un catalogo del que
 * depende Entrenador por clave foranea, y queda restringida a
 * ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<EspecialidadResponse>> listarPaginado(Pageable pageable) {
        return ResponseEntity.ok(especialidadService.listarPaginado(pageable));
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<EspecialidadResponse>> listarActivas() {
        return ResponseEntity.ok(especialidadService.listarTodasActivas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EspecialidadResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(especialidadService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EspecialidadResponse> crear(@Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EspecialidadResponse> editar(@PathVariable Long id, @Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.ok(especialidadService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        especialidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
