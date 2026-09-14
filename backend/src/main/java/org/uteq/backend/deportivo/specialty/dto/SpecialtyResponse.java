package org.uteq.backend.deportivo.specialty.dto;

import java.time.Instant;

/**
 * Vista de una especialidad de entrenador para el cliente.
 *
 * @param specialtyId  identificador de la especialidad
 * @param name         nombre de la especialidad
 * @param active       {@code true} si la especialidad está activa
 * @param createdAt    fecha de creación
 */
public record SpecialtyResponse(
        Long specialtyId,
        String name,
        Boolean active,
        Instant createdAt
) {}
