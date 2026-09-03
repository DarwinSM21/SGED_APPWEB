package org.uteq.backend.inventario.movimiento.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.inventario.articulo.repository.ArticuloRepository;
import org.uteq.backend.inventario.movimiento.dto.MovimientoDtos.*;
import org.uteq.backend.inventario.movimiento.entity.MovimientoStock;
import org.uteq.backend.inventario.movimiento.entity.MovimientoStock.TipoMovimiento;
import org.uteq.backend.inventario.movimiento.repository.MovimientoStockRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

/**
 * Entradas, salidas y ajustes de stock. {@code ENTRADA} y {@code AJUSTE}
 * suman a {@code stockActual}, {@code SALIDA} resta; una salida que dejaría
 * el stock negativo se rechaza antes de tocar la base de datos.
 */
@Service
@RequiredArgsConstructor
public class MovimientoStockService {
    private final MovimientoStockRepository movimientoStockRepository;
    private final ArticuloRepository articuloRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Lista paginada de movimientos, del más reciente al más antiguo.
     *
     * @param pageable paginación
     * @return la página, mapeada a {@link MovimientoResponse}
     */
    @Transactional(readOnly = true)
    public Page<MovimientoResponse> listarPaginado(Pageable pageable) {
        return movimientoStockRepository.findAllByOrderByFechaMovimientoDesc(pageable).map(this::toResponse);
    }

    /**
     * Movimientos de un artículo.
     *
     * @param idArticulo identificador del artículo
     * @param pageable   paginación
     * @return la página de movimientos del artículo
     */
    @Transactional(readOnly = true)
    public Page<MovimientoResponse> listarPorArticulo(Long idArticulo, Pageable pageable) {
        return movimientoStockRepository.findByArticulo_IdArticuloOrderByFechaMovimientoDesc(idArticulo, pageable)
                .map(this::toResponse);
    }

    /**
     * Registra un movimiento y ajusta el stock del artículo.
     *
     * @param request             artículo, tipo de movimiento, cantidad y
     *                            motivo
     * @param usernameRegistrador usuario que registra el movimiento
     * @return el movimiento registrado
     * @throws RecursoNoEncontradoException si el artículo no existe
     * @throws IllegalArgumentException     si una salida dejaría el stock
     *                                      negativo
     */
    @Auditado(accion = "CREAR", entidad = "MovimientoStock", idSpel = "#result.idMovimiento",
            descripcionSpel = "'registró ' + #result.tipoMovimiento + ' de ' + #result.cantidad + ' (' + #result.articulo + ')'")
    @Transactional
    public MovimientoResponse registrar(MovimientoRequest request, String usernameRegistrador) {
        Articulo articulo = buscarArticulo(request.idArticulo());
        Usuario registrador = buscarUsuario(usernameRegistrador);

        int delta = request.tipoMovimiento() == TipoMovimiento.SALIDA
                ? -request.cantidad()
                : request.cantidad();
        int nuevoStock = articulo.getStockActual() + delta;

        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente: hay " + articulo.getStockActual() + " unidades de \""
                            + articulo.getNombre() + "\" y se intentan retirar " + request.cantidad());
        }

        articulo.setStockActual(nuevoStock);
        articuloRepository.save(articulo);

        MovimientoStock movimiento = MovimientoStock.builder()
                .articulo(articulo)
                .tipoMovimiento(request.tipoMovimiento())
                .cantidad(request.cantidad())
                .motivo(request.motivo())
                .registradoPor(registrador)
                .build();

        return toResponse(movimientoStockRepository.save(movimiento));
    }

    private Articulo buscarArticulo(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Artículo no encontrado con ID: " + id));
    }

    private Usuario buscarUsuario(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + username));
    }

    private MovimientoResponse toResponse(MovimientoStock m) {
        var registrador = m.getRegistradoPor().getPersona();
        return new MovimientoResponse(
                m.getIdMovimiento(),
                m.getArticulo().getIdArticulo(),
                m.getArticulo().getNombre(),
                m.getTipoMovimiento(),
                m.getCantidad(),
                m.getMotivo(),
                registrador.getNombre() + " " + registrador.getApellido(),
                m.getFechaMovimiento()
        );
    }
}
