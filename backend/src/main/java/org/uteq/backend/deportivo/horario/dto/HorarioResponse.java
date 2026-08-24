package org.uteq.backend.deportivo.horario.dto;

import java.time.LocalTime;

public record HorarioResponse(
        Long idHorario,
        /** Se expone junto al nombre para que la pantalla de edicion pueda
         *  preseleccionar la categoria sin emparejar por texto. */
        Long idCategoria,
        String categoria,
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String campo,
        String descripcion,
        Boolean activo
) {}
