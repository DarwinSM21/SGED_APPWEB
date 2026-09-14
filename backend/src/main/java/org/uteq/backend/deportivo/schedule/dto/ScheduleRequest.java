package org.uteq.backend.deportivo.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Datos para crear o editar un horario recurrente de entrenamiento.
 *
 * @param categoryId   identificador de la categoría a la que pertenece
 * @param dayOfWeek    día de la semana (1=lunes a 7=domingo)
 * @param startTime    hora de inicio
 * @param endTime      hora de fin
 * @param field        cancha o lugar, opcional
 * @param description  descripción del horario, opcional
 */
public record ScheduleRequest(
        @NotNull Long categoryId,
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String field,
        String description
) {}
