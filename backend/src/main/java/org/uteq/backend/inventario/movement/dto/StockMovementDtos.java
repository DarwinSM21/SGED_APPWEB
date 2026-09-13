package org.uteq.backend.inventario.movement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.inventario.movement.entity.StockMovement.MovementType;

import java.time.Instant;

public final class StockMovementDtos {

    private StockMovementDtos() {}

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
