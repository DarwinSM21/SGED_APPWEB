package org.uteq.backend.deportivo.schedule.dto;

import java.time.LocalTime;

public record ScheduleResponse(
        Long idHorario,
        Long idCategoria,
        String categoria,
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String campo,
        String descripcion,
        Boolean activo,
        String chocaCon
) {}
