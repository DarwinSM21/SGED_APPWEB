package org.uteq.backend.inventario.item.dto;

import jakarta.validation.constraints.*;
import org.uteq.backend.inventario.item.entity.Item.ItemType;

import java.time.Instant;
import java.util.List;

/** Contenedor de los DTO de artículos del inventario. */
public final class ItemDtos {

    private ItemDtos() {}

    /**
     * Datos para crear o editar un artículo del inventario.
     *
     * @param name           nombre del artículo
     * @param type           tipo de artículo
     * @param size           talla, opcional
     * @param description    descripción, opcional
     * @param minimumStock   stock mínimo antes de generar alerta
     * @param unitOfMeasure  unidad de medida, opcional
     */
    public record ItemRequest(
            @NotBlank(message = "El nombre del artículo es obligatorio")
            @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
            String name,

            @NotNull(message = "El tipo de artículo es obligatorio")
            ItemType type,

            @Size(max = 20, message = "La talla no puede superar los 20 caracteres")
            String size,

            @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
            String description,

            @NotNull(message = "El stock mínimo es obligatorio")
            @Min(value = 0, message = "El stock mínimo no puede ser negativo")
            Integer minimumStock,

            @Size(max = 20, message = "La unidad de medida no puede superar los 20 caracteres")
            String unitOfMeasure
    ) {}

    /**
     * Vista de un artículo del inventario para el cliente.
     *
     * @param itemId          identificador del artículo
     * @param name            nombre del artículo
     * @param type            tipo de artículo
     * @param size            talla
     * @param description     descripción
     * @param currentStock    stock disponible actual
     * @param minimumStock    stock mínimo configurado
     * @param unitOfMeasure   unidad de medida
     * @param active          {@code true} si el artículo está activo
     * @param createdAt       fecha de creación
     */
    public record ItemResponse(
            Long itemId,
            String name,
            ItemType type,
            String size,
            String description,
            Integer currentStock,
            Integer minimumStock,
            String unitOfMeasure,
            Boolean active,
            Instant createdAt
    ) {}

    /**
     * Listado de artículos con stock por debajo del mínimo configurado.
     *
     * @param totalElements  cantidad de artículos en bajo stock
     * @param items          detalle de cada artículo
     */
    public record LowStockResponse(
            long totalElements,
            List<ItemResponse> items
    ) {}
}
