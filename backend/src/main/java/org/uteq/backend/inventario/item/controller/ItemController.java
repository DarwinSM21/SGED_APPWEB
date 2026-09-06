package org.uteq.backend.inventario.item.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.inventario.item.dto.ItemDtos.*;
import org.uteq.backend.inventario.item.service.ItemService;

import java.util.List;

/**
 * CRUD del catálogo de artículos de inventario. La lectura la necesitan los
 * tres roles operativos; la escritura queda restringida a
 * {@code ADMINISTRADOR} y {@code RECEPCIONISTA} (quienes gestionan el
 * depósito físico). {@code ENTRENADOR} solo consulta el catálogo.
 */
@RestController
@RequestMapping("/api/inventario/articulos")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService articuloService;

    /**
     * Lista paginada de artículos.
     *
     * @param pageable paginación y orden
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    public ResponseEntity<Page<ItemResponse>> listPaged(Pageable pageable) {
        return ResponseEntity.ok(articuloService.listPaged(pageable));
    }

    /**
     * Lista completa de artículos activos, sin paginar (para desplegables).
     *
     * @return {@code 200 OK} con todos los artículos activos
     */
    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    public ResponseEntity<List<ItemResponse>> listActive() {
        return ResponseEntity.ok(articuloService.listActive());
    }

    /**
     * Artículos cuyo stock está en o por debajo del mínimo.
     *
     * @return {@code 200 OK} con el total y el listado de artículos en
     *         stock bajo
     */
    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<LowStockResponse> lowStock() {
        return ResponseEntity.ok(articuloService.lowStock());
    }

    /**
     * Busca un artículo por su identificador.
     *
     * @param id identificador del artículo
     * @return {@code 200 OK} con el artículo
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR')")
    public ResponseEntity<ItemResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.findById(id));
    }

    /**
     * Crea un artículo (con {@code stockActual} en 0).
     *
     * @param request datos del artículo; validado con {@code @Valid}
     * @return {@code 201 Created} con el artículo creado
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articuloService.create(request));
    }

    /**
     * Actualiza los datos de un artículo (el stock no se toca aquí).
     *
     * @param id      identificador del artículo a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con el artículo actualizado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ItemResponse> update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(articuloService.update(id, request));
    }

    /**
     * Baja lógica de un artículo.
     *
     * @param id identificador del artículo
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articuloService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva un artículo dado de baja.
     *
     * @param id identificador del artículo
     * @return {@code 200 OK} con el artículo reactivado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si ya está activo ({@code 422})
     */
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ItemResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.reactivate(id));
    }
}
