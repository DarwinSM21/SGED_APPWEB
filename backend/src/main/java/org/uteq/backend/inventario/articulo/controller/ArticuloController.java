package org.uteq.backend.inventario.articulo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.inventario.articulo.dto.ArticuloDtos.*;
import org.uteq.backend.inventario.articulo.service.ArticuloService;

import java.util.List;

/**
 * CRUD del catalogo de articulos de inventario. La lectura la necesitan
 * los 3 roles operativos; la escritura queda restringida a ADMINISTRADOR
 * y RECEPCIONISTA (quienes gestionan el deposito fisico). ENTRENADOR solo
 * consulta el catalogo para asignar articulos, nunca lo modifica.
 */
@RestController
@RequestMapping("/api/inventario/articulos")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService articuloService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    public ResponseEntity<Page<ArticuloResponse>> listarPaginado(Pageable pageable) {
        return ResponseEntity.ok(articuloService.listarPaginado(pageable));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    public ResponseEntity<List<ArticuloResponse>> listarActivos() {
        return ResponseEntity.ok(articuloService.listarActivos());
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<StockBajoResponse> stockBajo() {
        return ResponseEntity.ok(articuloService.stockBajo());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    public ResponseEntity<ArticuloResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ArticuloResponse> crear(@Valid @RequestBody ArticuloRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articuloService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ArticuloResponse> editar(@PathVariable Long id, @Valid @RequestBody ArticuloRequest request) {
        return ResponseEntity.ok(articuloService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        articuloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
