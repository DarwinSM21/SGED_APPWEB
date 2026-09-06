package org.uteq.backend.inventario.item.dto;

import jakarta.validation.constraints.*;
import org.uteq.backend.inventario.item.entity.Item.ItemType;

import java.time.Instant;
import java.util.List;

public final class ItemDtos {

    private ItemDtos() {}

    public record ItemRequest(
            @NotBlank(message = "El nombre del artículo es obligatorio")
            @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
            String nombre,

            @NotNull(message = "El tipo de artículo es obligatorio")
            ItemType tipo,

            @Size(max = 20, message = "La talla no puede superar los 20 caracteres")
            String talla,

            @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
            String descripcion,

            @NotNull(message = "El stock mínimo es obligatorio")
            @Min(value = 0, message = "El stock mínimo no puede ser negativo")
            Integer stockMinimo,

            @Size(max = 20, message = "La unidad de medida no puede superar los 20 caracteres")
            String unidadMedida
    ) {}

    public record ItemResponse(
            Long idArticulo,
            String nombre,
            ItemType tipo,
            String talla,
            String descripcion,
            Integer stockActual,
            Integer stockMinimo,
            String unidadMedida,
            Boolean activo,
            Instant createdAt
    ) {}

    public record LowStockResponse(
            long total,
            List<ItemResponse> articulos
    ) {}
}
