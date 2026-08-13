package org.uteq.backend.inventario.movimiento.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.inventario.movimiento.dto.MovimientoDtos.*;
import org.uteq.backend.inventario.movimiento.service.MovimientoStockService;

/**
 * Entradas/salidas/ajustes de stock. El registro queda restringido a
 * ADMINISTRADOR y RECEPCIONISTA, quienes manejan el deposito fisico;
 * ENTRENADOR solo consulta el historial.
 */
@RestController
@RequestMapping("/api/inventario/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockService movimientoStockService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<MovimientoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(movimientoStockService.listarPaginado(pageable));
    }

    @GetMapping("/articulo/{idArticulo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<MovimientoResponse>> listarPorArticulo(@PathVariable Long idArticulo, Pageable pageable) {
        return ResponseEntity.ok(movimientoStockService.listarPorArticulo(idArticulo, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<MovimientoResponse> registrar(@Valid @RequestBody MovimientoRequest request) {
        var movimiento = movimientoStockService.registrar(request, usernameAutenticado());
        return ResponseEntity.status(HttpStatus.CREATED).body(movimiento);
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
