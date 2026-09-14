package org.uteq.backend.deportivo.session.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Sesión de entrenamiento del día para el panel del entrenador.
 *
 * @param sessionId      identificador de la sesión
 * @param category       nombre de la categoría
 * @param coach          nombre del entrenador a cargo
 * @param date           fecha de la sesión
 * @param startTime      hora de inicio
 * @param endTime        hora de fin
 * @param field          cancha o lugar
 * @param status         estado de la sesión
 * @param hasEvaluation  {@code true} si ya tiene evaluación diaria iniciada
 */
public record SessionTodayResponse(
        Long sessionId,
        String category,
        String coach,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String field,
        String status,
        boolean hasEvaluation
) {}
