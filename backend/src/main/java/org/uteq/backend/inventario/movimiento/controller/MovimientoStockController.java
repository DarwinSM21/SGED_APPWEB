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
 * Entradas, salidas y ajustes de stock. El registro queda restringido a
 * {@code ADMINISTRADOR} y {@code RECEPCIONISTA}, quienes manejan el depósito
 * físico; {@code ENTRENADOR} solo consulta el historial.
 */
@RestController
@RequestMapping("/api/inventario/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {
    private final MovimientoStockService movimientoStockService;

    /**
     * Lista paginada de movimientos, del más reciente al más antiguo.
     *
     * @param pageable paginación
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<MovimientoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(movimientoStockService.listarPaginado(pageable));
    }

    /**
     * Movimientos de un artículo.
     *
     * @param idArticulo identificador del artículo
     * @param pageable   paginación
     * @return {@code 200 OK} con la página de movimientos del artículo
     */
    @GetMapping("/articulo/{idArticulo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<MovimientoResponse>> listarPorArticulo(@PathVariable Long idArticulo, Pageable pageable) {
        return ResponseEntity.ok(movimientoStockService.listarPorArticulo(idArticulo, pageable));
    }

    /**
     * Registra un movimiento y ajusta el stock del artículo
     * ({@code ENTRADA}/{@code AJUSTE} suman, {@code SALIDA} resta).
     *
     * @param request artículo, tipo, cantidad y motivo; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el movimiento registrado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el artículo no existe ({@code 404})
     * @throws IllegalArgumentException si una salida dejaría el stock
     *         negativo ({@code 422})
     */
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
