package org.uteq.backend.deportivo.category.dto;

import java.time.Instant;

public record CategoryResponse(
        Long idCategoria,
        String nombre,
        Short edadMin,
        Short edadMax,
        String descripcion,
        Boolean activo,
        Instant createdAt
) {}