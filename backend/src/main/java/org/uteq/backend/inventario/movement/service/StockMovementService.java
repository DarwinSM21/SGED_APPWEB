package org.uteq.backend.inventario.movement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.inventario.item.entity.Item;
import org.uteq.backend.inventario.item.repository.ItemRepository;
import org.uteq.backend.inventario.movement.dto.StockMovementDtos.*;
import org.uteq.backend.inventario.movement.entity.StockMovement;
import org.uteq.backend.inventario.movement.entity.StockMovement.MovementType;
import org.uteq.backend.inventario.movement.repository.StockMovementRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

/**
 * Entradas, salidas y ajustes de stock. {@code ENTRADA} y {@code AJUSTE}
 * suman a {@code stockActual}, {@code SALIDA} resta; una salida que dejaría
 * el stock negativo se rechaza antes de tocar la base de datos.
 */
@Service
@RequiredArgsConstructor
public class StockMovementService {
    private final StockMovementRepository movimientoStockRepository;
    private final ItemRepository articuloRepository;
    private final UserAccountRepository usuarioRepository;

    /**
     * Lista paginada de movimientos, del más reciente al más antiguo.
     *
     * @param pageable paginación
     * @return la página, mapeada a {@link StockMovementResponse}
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> listPaged(Pageable pageable) {
        return movimientoStockRepository.findAllByOrderByMovementDateDesc(pageable).map(this::toResponse);
    }

    /**
     * Movimientos de un artículo.
     *
     * @param idArticulo identificador del artículo
     * @param pageable   paginación
     * @return la página de movimientos del artículo
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> listByItem(Long idArticulo, Pageable pageable) {
        return movimientoStockRepository.findByItem_IdOrderByMovementDateDesc(idArticulo, pageable)
                .map(this::toResponse);
    }

    /**
     * Registra un movimiento y ajusta el stock del artículo.
     *
     * @param request             artículo, tipo de movimiento, cantidad y
     *                            motivo
     * @param usernameRegistrador usuario que registra el movimiento
     * @return el movimiento registrado
     * @throws ResourceNotFoundException si el artículo no existe
     * @throws IllegalArgumentException     si una salida dejaría el stock
     *                                      negativo
     */
    @Audited(action = "CREAR", entity = "MovimientoStock", idSpel = "#result.idMovimiento",
            descriptionSpel = "'registró ' + #result.tipoMovimiento + ' de ' + #result.cantidad + ' (' + #result.articulo + ')'")
    @Transactional
    public StockMovementResponse register(StockMovementRequest request, String usernameRegistrador) {
        Item articulo = findItem(request.idArticulo());
        UserAccount registrador = findUser(usernameRegistrador);

        int delta = request.tipoMovimiento() == MovementType.SALIDA
                ? -request.cantidad()
                : request.cantidad();
        int nuevoStock = articulo.getCurrentStock() + delta;

        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente: hay " + articulo.getCurrentStock() + " unidades de \""
                            + articulo.getName() + "\" y se intentan retirar " + request.cantidad());
        }

        articulo.setCurrentStock(nuevoStock);
        articuloRepository.save(articulo);

        StockMovement movimiento = StockMovement.builder()
                .item(articulo)
                .movementType(request.tipoMovimiento())
                .quantity(request.cantidad())
                .reason(request.motivo())
                .registeredBy(registrador)
                .build();

        return toResponse(movimientoStockRepository.save(movimiento));
    }

    private Item findItem(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));
    }

    private UserAccount findUser(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + username));
    }

    private StockMovementResponse toResponse(StockMovement m) {
        var registrador = m.getRegisteredBy().getPerson();
        return new StockMovementResponse(
                m.getId(),
                m.getItem().getId(),
                m.getItem().getName(),
                m.getMovementType(),
                m.getQuantity(),
                m.getReason(),
                registrador.getName() + " " + registrador.getLastName(),
                m.getMovementDate()
        );
    }
}
