package org.uteq.backend.deportivo.specialty.dto;

import java.time.Instant;

public record SpecialtyResponse(
        Long idEspecialidad,
        String nombre,
        Boolean activo,
        Instant createdAt
) {}
