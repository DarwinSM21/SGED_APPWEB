package org.uteq.backend.deportivo.sesion.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SesionHoyResponse(
        Long idSesion,
        String categoria,
        String entrenador,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String campo,
        String estado,
        boolean tieneEvaluacion
) {}
