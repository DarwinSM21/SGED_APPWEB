package org.uteq.backend.deportivo.horario.dto;

import java.time.LocalTime;

public record HorarioResponse(
        Long idHorario,
        String categoria,
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String campo,
        String descripcion,
        Boolean activo
) {}
