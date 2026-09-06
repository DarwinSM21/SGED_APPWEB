package org.uteq.backend.academico.student.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StudentResponse(
        Long idEstudiante,
        Long idPersona,
        Long idCategoria,
        Long idEstadoGeneral,
        String nombrePersona,
        String apellidoPersona,
        String nombreCategoria,
        String nombreEstadoGeneral,
        String codigoEstudiante,
        LocalDate fechaIngreso,
        BigDecimal peso,
        BigDecimal altura,
        Long idPosicion,
        String nombrePosicion,
        String abreviaturaPosicion,
        Boolean activo,
        Instant createdAt
) {}
