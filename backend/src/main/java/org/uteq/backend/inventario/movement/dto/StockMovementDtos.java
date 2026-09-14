package org.uteq.backend.inventario.movement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.inventario.movement.entity.StockMovement.MovementType;

import java.time.Instant;

/** Contenedor de los DTO de movimientos de stock. */
public final class StockMovementDtos {

    private StockMovementDtos() {}

    /**
     * Datos para registrar un movimiento de stock (entrada o salida).
     *
     * @param itemId        identificador del artículo
     * @param movementType  tipo de movimiento (entrada o salida)
     * @param quantity      cantidad movida
     * @param reason        motivo del movimiento, opcional
     */
    public record StockMovementRequest(
            @NotNull(message = "El artículo es obligatorio")
            Long itemId,

            @NotNull(message = "El tipo de movimiento es obligatorio")
            MovementType movementType,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            Integer quantity,

            @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
            String reason
    ) {}

    /**
     * Vista de un movimiento de stock para el cliente.
     *
     * @param movementId    identificador del movimiento
     * @param itemId        identificador del artículo
     * @param item          nombre del artículo
     * @param movementType  tipo de movimiento
     * @param quantity      cantidad movida
     * @param reason        motivo del movimiento
     * @param registeredBy  usuario que registró el movimiento
     * @param movementDate  fecha y hora del movimiento
     */
    public record StockMovementResponse(
            Long movementId,
            Long itemId,
            String item,
            MovementType movementType,
            Integer quantity,
            String reason,
            String registeredBy,
            Instant movementDate
    ) {}
}
