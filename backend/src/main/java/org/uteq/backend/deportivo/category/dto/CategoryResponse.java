package org.uteq.backend.deportivo.category.dto;

import java.time.Instant;

public record CategoryResponse(
        Long categoryId,
        String name,
        Short minAge,
        Short maxAge,
        String description,
        Boolean active,
        Instant createdAt
) {}