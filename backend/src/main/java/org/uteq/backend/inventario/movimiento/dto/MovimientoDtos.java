package org.uteq.backend.inventario.movimiento.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.inventario.movimiento.entity.MovimientoStock.TipoMovimiento;

import java.time.Instant;

public final class MovimientoDtos {

    private MovimientoDtos() {}

    public record MovimientoRequest(
            @NotNull(message = "El artículo es obligatorio")
            Long idArticulo,

            @NotNull(message = "El tipo de movimiento es obligatorio")
            TipoMovimiento tipoMovimiento,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            Integer cantidad,

            @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
            String motivo
    ) {}

    public record MovimientoResponse(
            Long idMovimiento,
            Long idArticulo,
            String articulo,
            TipoMovimiento tipoMovimiento,
            Integer cantidad,
            String motivo,
            String registradoPor,
            Instant fechaMovimiento
    ) {}
}
