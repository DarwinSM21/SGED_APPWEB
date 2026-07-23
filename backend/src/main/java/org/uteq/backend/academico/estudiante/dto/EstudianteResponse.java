package org.uteq.backend.academico.estudiante.dto;

import java.time.Instant;

public record EstudianteResponse(
        Long idEstudiante,
        String nombre,
        String apellido,
        String categoria,
        Boolean activo,
        Instant creadoEn
) {}
