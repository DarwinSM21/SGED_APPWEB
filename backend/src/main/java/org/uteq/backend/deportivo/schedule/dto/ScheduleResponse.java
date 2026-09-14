package org.uteq.backend.deportivo.schedule.dto;

import java.time.LocalTime;

/**
 * Vista de un horario recurrente para el cliente.
 *
 * @param scheduleId     identificador del horario
 * @param categoryId     identificador de la categoría
 * @param category       nombre de la categoría
 * @param dayOfWeek      día de la semana (1=lunes a 7=domingo)
 * @param startTime      hora de inicio
 * @param endTime        hora de fin
 * @param field          cancha o lugar
 * @param description    descripción del horario
 * @param active         {@code true} si el horario está activo
 * @param conflictsWith  descripción de otro horario en conflicto, si lo hay
 */
public record ScheduleResponse(
        Long scheduleId,
        Long categoryId,
        String category,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String field,
        String description,
        Boolean active,
        String conflictsWith
) {}
