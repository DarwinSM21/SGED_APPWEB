package org.uteq.backend.deportivo.session.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionTodayResponse(
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
