package org.uteq.backend.deportivo.sesion.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Sesion de entrenamiento, tal como la necesita el punto de entrada del
 * entrenador: lo suficiente para decidir a cual entrar, no el detalle
 * completo (eso lo da GET /api/evaluaciones/sesion/{idSesion}). Se reutiliza
 * tanto para /hoy (donde fecha siempre es la de hoy) como para /mias (el
 * historial completo, donde fecha si varia por fila).
 */
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
