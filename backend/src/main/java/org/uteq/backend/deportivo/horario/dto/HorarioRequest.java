package org.uteq.backend.deportivo.horario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * El idEntrenador nunca viene del cliente: se resuelve en el controller
 * desde el usuario autenticado, mismo criterio que SesionCrearRequest.
 */
public record HorarioRequest(
        @NotNull Long idCategoria,
        @NotNull @Min(1) @Max(7) Integer diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        String campo,
        String descripcion
) {}
