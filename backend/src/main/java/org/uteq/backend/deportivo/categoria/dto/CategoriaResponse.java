package org.uteq.backend.deportivo.categoria.dto;

import java.time.Instant;

public record CategoriaResponse(
        Long idCategoria,
        String nombre,
        Short edadMin,
        Short edadMax,
        String descripcion,
        Boolean activo,
        Instant createdAt
) {}