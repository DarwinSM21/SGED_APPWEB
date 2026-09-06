package org.uteq.backend.inventario.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.seguridad.audit.aop.Audited;
import org.uteq.backend.inventario.item.dto.ItemDtos.*;
import org.uteq.backend.inventario.item.entity.Item;
import org.uteq.backend.inventario.item.repository.ItemRepository;

import java.util.List;

/**
 * Lógica de negocio del catálogo de artículos de inventario. El
 * {@code stockActual} no se modifica aquí: solo por movimientos de stock o
 * asignaciones. Las bajas son lógicas ({@code activo = false}).
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository articuloRepository;

    /**
     * Lista paginada de artículos.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, mapeada a {@link ItemResponse}
     */
    @Transactional(readOnly = true)
    public Page<ItemResponse> listPaged(Pageable pageable) {
        return articuloRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Lista completa de artículos activos, sin paginar.
     *
     * @return todos los artículos activos
     */
    @Transactional(readOnly = true)
    public List<ItemResponse> listActive() {
        return articuloRepository.findByActivoTrue().stream().map(this::toResponse).toList();
    }

    /**
     * Busca un artículo por su identificador.
     *
     * @param id identificador del artículo
     * @return el artículo encontrado
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public ItemResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    /**
     * Crea un artículo con {@code stockActual} en 0.
     *
     * @param request datos del artículo
     * @return el artículo creado
     */
    @Audited(accion = "CREAR", entidad = "Articulo", idSpel = "#result.idArticulo",
            descripcionSpel = "'creó el artículo ' + #result.nombre")
    @Transactional
    public ItemResponse create(ItemRequest request) {
        Item articulo = Item.builder()
                .nombre(request.nombre())
                .tipo(request.tipo())
                .talla(request.talla())
                .descripcion(request.descripcion())
                .stockActual(0)
                .stockMinimo(request.stockMinimo())
                .unidadMedida(request.unidadMedida() != null && !request.unidadMedida().isBlank()
                        ? request.unidadMedida() : "unidad")
                .activo(true)
                .build();

        return toResponse(articuloRepository.save(articulo));
    }

    /**
     * Actualiza los datos de un artículo (no el stock).
     *
     * @param id      identificador del artículo a editar
     * @param request datos nuevos
     * @return el artículo actualizado
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(accion = "EDITAR", entidad = "Articulo", idSpel = "#result.idArticulo",
            descripcionSpel = "'editó el artículo ' + #result.nombre")
    @Transactional
    public ItemResponse update(Long id, ItemRequest request) {
        Item articulo = findEntity(id);

        articulo.setNombre(request.nombre());
        articulo.setTipo(request.tipo());
        articulo.setTalla(request.talla());
        articulo.setDescripcion(request.descripcion());
        articulo.setStockMinimo(request.stockMinimo());
        if (request.unidadMedida() != null && !request.unidadMedida().isBlank()) {
            articulo.setUnidadMedida(request.unidadMedida());
        }

        return toResponse(articuloRepository.save(articulo));
    }

    /**
     * Baja lógica de un artículo ({@code activo = false}).
     *
     * @param id identificador del artículo
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(accion = "ELIMINAR", entidad = "Articulo", idSpel = "#p0",
            descripcionSpel = "'desactivó el artículo #' + #p0")
    @Transactional
    public void delete(Long id) {
        Item articulo = findEntity(id);
        articulo.setActivo(false);
        articuloRepository.save(articulo);
    }

    /**
     * Reactiva un artículo dado de baja.
     *
     * @param id identificador del artículo
     * @return el artículo reactivado
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si ya está activo
     */
    @Audited(accion = "REACTIVAR", entidad = "Articulo", idSpel = "#p0",
            descripcionSpel = "'reactivo el articulo #' + #p0")
    @Transactional
    public ItemResponse reactivate(Long id) {
        Item articulo = findEntity(id);

        if (Boolean.TRUE.equals(articulo.getActivo())) {
            throw new IllegalArgumentException("El articulo ya se encuentra activo");
        }

        articulo.setActivo(true);
        return toResponse(articuloRepository.save(articulo));
    }

    /**
     * Artículos en o por debajo de su stock mínimo.
     *
     * @return el total y el listado de artículos en stock bajo
     */
    @Transactional(readOnly = true)
    public LowStockResponse lowStock() {
        List<ItemResponse> articulos = articuloRepository.findLowStock().stream()
                .map(this::toResponse)
                .toList();
        Long total = articuloRepository.countLowStock();
        return new LowStockResponse(total != null ? total : 0L, articulos);
    }

    private Item findEntity(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));
    }

    private ItemResponse toResponse(Item a) {
        return new ItemResponse(
                a.getIdArticulo(),
                a.getNombre(),
                a.getTipo(),
                a.getTalla(),
                a.getDescripcion(),
                a.getStockActual(),
                a.getStockMinimo(),
                a.getUnidadMedida(),
                a.getActivo(),
                a.getCreatedAt()
        );
    }
}
