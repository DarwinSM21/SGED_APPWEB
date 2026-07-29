package org.uteq.backend.academico.estudiante.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EstudianteResponse(
        Long idEstudiante,
        String nombrePersona,
        String apellidoPersona,
        String nombreCategoria,
        String nombreEstadoGeneral,
        String codigoEstudiante,
        LocalDate fechaIngreso,
        BigDecimal peso,
        BigDecimal altura,
        Boolean activo,
        Instant createdAt
) {}