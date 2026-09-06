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
            Long idArticulo,

            @NotNull(message = "El tipo de movimiento es obligatorio")
            MovementType tipoMovimiento,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            Integer cantidad,

            @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
            String motivo
    ) {}

    public record StockMovementResponse(
            Long idMovimiento,
            Long idArticulo,
            String articulo,
            MovementType tipoMovimiento,
            Integer cantidad,
            String motivo,
            String registradoPor,
            Instant fechaMovimiento
    ) {}
}
