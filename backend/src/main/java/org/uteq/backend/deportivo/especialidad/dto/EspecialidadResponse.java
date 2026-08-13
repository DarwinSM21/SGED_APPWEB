package org.uteq.backend.deportivo.especialidad.dto;

import java.time.Instant;

public record EspecialidadResponse(
        Long idEspecialidad,
        String nombre,
        Boolean activo,
        Instant createdAt
) {}
