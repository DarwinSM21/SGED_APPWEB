package org.uteq.backend.deportivo.category.dto;

import java.time.Instant;

/**
 * Vista de una categoría deportiva para el cliente.
 *
 * @param categoryId   identificador de la categoría
 * @param name         nombre de la categoría
 * @param minAge       edad mínima permitida
 * @param maxAge       edad máxima permitida
 * @param description  descripción de la categoría
 * @param active       {@code true} si la categoría está activa
 * @param createdAt    fecha de creación
 */
public record CategoryResponse(
        Long categoryId,
        String name,
        Short minAge,
        Short maxAge,
        String description,
        Boolean active,
        Instant createdAt
) {}