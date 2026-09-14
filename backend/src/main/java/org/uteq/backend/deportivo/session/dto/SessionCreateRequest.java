package org.uteq.backend.deportivo.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos para crear una sesión de entrenamiento puntual (fuera del horario recurrente).
 *
 * @param categoryId  identificador de la categoría
 * @param date        fecha de la sesión
 * @param startTime   hora de inicio
 * @param endTime     hora de fin
 * @param field       cancha o lugar, opcional
 */
public record SessionCreateRequest(
        @NotNull Long categoryId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String field
) {}
