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

@Service
@RequiredArgsConstructor
public class ArticuloService {

    private final ArticuloRepository articuloRepository;

    @Transactional(readOnly = true)
    public Page<ArticuloResponse> listarPaginado(Pageable pageable) {
        return articuloRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ArticuloResponse> listarActivos() {
        return articuloRepository.findByActivoTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ArticuloResponse buscarPorId(Long id) {
        return toResponse(buscarEntidad(id));
    }

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

    @Auditado(accion = "ELIMINAR", entidad = "Articulo", idSpel = "#p0",
            descripcionSpel = "'desactivó el artículo #' + #p0")
    @Transactional
    public void eliminar(Long id) {
        Articulo articulo = buscarEntidad(id);
        articulo.setActivo(false);
        articuloRepository.save(articulo);
    }

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
