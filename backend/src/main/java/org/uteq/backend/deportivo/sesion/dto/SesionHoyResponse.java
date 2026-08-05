package org.uteq.backend.deportivo.sesion.dto;

import java.time.LocalTime;

/**
 * Sesion de entrenamiento del dia, tal como la necesita el punto de entrada
 * del entrenador: lo suficiente para decidir a cual entrar, no el detalle
 * completo (eso lo da GET /api/evaluaciones/sesion/{idSesion}).
 */
public record SesionHoyResponse(
        Long idSesion,
        String categoria,
        String entrenador,
        LocalTime horaInicio,
        LocalTime horaFin,
        String campo,
        String estado,
        boolean tieneEvaluacion
) {}
