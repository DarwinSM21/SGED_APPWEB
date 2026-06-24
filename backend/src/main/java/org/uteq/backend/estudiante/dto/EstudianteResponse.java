package org.uteq.backend.estudiante.dto;

import java.time.Instant;

public record EstudianteResponse(
        Long id,
        String nombre,
        String apellido,
        String categoria,
        Boolean activo,
        Instant creadoEn
) {}