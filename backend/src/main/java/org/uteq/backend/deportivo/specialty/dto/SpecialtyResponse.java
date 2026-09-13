package org.uteq.backend.deportivo.specialty.dto;

import java.time.Instant;

public record SpecialtyResponse(
        Long specialtyId,
        String name,
        Boolean active,
        Instant createdAt
) {}
