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
        Boolean activo,
        /**
         * Con que otro horario se cruza, si se cruza. null cuando esta bien.
         *
         * <p>Validar el alta no arregla lo que ya estaba cargado: en esta base
         * habia tres horarios del mismo entrenador a las 16:00 que llevaban
         * meses generando sesiones imposibles. Se marca en la lista que el
         * entrenador ya mira, para que pueda corregirlo sin que nadie le
         * explique que hay un problema.
         */
        String chocaCon
) {}
