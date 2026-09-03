package org.uteq.backend.inventario.articulo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.inventario.articulo.dto.ArticuloDtos.*;
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.inventario.articulo.repository.ArticuloRepository;

import java.util.List;

/**
 * Lógica de negocio del catálogo de artículos de inventario. El
 * {@code stockActual} no se modifica aquí: solo por movimientos de stock o
 * asignaciones. Las bajas son lógicas ({@code activo = false}).
 */
@Service
@RequiredArgsConstructor
public class ArticuloService {

    private final ArticuloRepository articuloRepository;

    /**
     * Lista paginada de artículos.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, mapeada a {@link ArticuloResponse}
     */
    @Transactional(readOnly = true)
    public Page<ArticuloResponse> listarPaginado(Pageable pageable) {
        return articuloRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Lista completa de artículos activos, sin paginar.
     *
     * @return todos los artículos activos
     */
    @Transactional(readOnly = true)
    public List<ArticuloResponse> listarActivos() {
        return articuloRepository.findByActivoTrue().stream().map(this::toResponse).toList();
    }

    /**
     * Busca un artículo por su identificador.
     *
     * @param id identificador del artículo
     * @return el artículo encontrado
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public ArticuloResponse buscarPorId(Long id) {
        return toResponse(buscarEntidad(id));
    }

    /**
     * Crea un artículo con {@code stockActual} en 0.
     *
     * @param request datos del artículo
     * @return el artículo creado
     */
    @Auditado(accion = "CREAR", entidad = "Articulo", idSpel = "#result.idArticulo",
            descripcionSpel = "'creó el artículo ' + #result.nombre")
    @Transactional
    public ArticuloResponse crear(ArticuloRequest request) {
        Articulo articulo = Articulo.builder()
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
     * @throws RecursoNoEncontradoException si no existe
     */
    @Auditado(accion = "EDITAR", entidad = "Articulo", idSpel = "#result.idArticulo",
            descripcionSpel = "'editó el artículo ' + #result.nombre")
    @Transactional
    public ArticuloResponse editar(Long id, ArticuloRequest request) {
        Articulo articulo = buscarEntidad(id);

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
     * @throws RecursoNoEncontradoException si no existe
     */
    @Auditado(accion = "ELIMINAR", entidad = "Articulo", idSpel = "#p0",
            descripcionSpel = "'desactivó el artículo #' + #p0")
    @Transactional
    public void eliminar(Long id) {
        Articulo articulo = buscarEntidad(id);
        articulo.setActivo(false);
        articuloRepository.save(articulo);
    }

    /**
     * Reactiva un artículo dado de baja.
     *
     * @param id identificador del artículo
     * @return el artículo reactivado
     * @throws RecursoNoEncontradoException si no existe
     * @throws IllegalArgumentException     si ya está activo
     */
    @Auditado(accion = "REACTIVAR", entidad = "Articulo", idSpel = "#p0",
            descripcionSpel = "'reactivo el articulo #' + #p0")
    @Transactional
    public ArticuloResponse reactivar(Long id) {
        Articulo articulo = buscarEntidad(id);

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
    public StockBajoResponse stockBajo() {
        List<ArticuloResponse> articulos = articuloRepository.findConStockBajo().stream()
                .map(this::toResponse)
                .toList();
        Long total = articuloRepository.contarStockBajo();
        return new StockBajoResponse(total != null ? total : 0L, articulos);
    }

    private Articulo buscarEntidad(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Artículo no encontrado con ID: " + id));
    }

    private ArticuloResponse toResponse(Articulo a) {
        return new ArticuloResponse(
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
