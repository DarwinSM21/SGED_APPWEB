package org.uteq.backend.deportivo.match.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Contenedor de los DTO de alineación de un partido. */
public class LineupDtos {
    /**
     * Un jugador dentro de la alineación a guardar.
     *
     * @param studentId  identificador del estudiante
     * @param positionId posición asignada, opcional
     * @param starter    {@code true} si va de titular
     */
    public record PlayerOnField(
            @NotNull Long studentId,
            Long positionId,
            @NotNull Boolean starter
    ) {}

    /**
     * Alineación a guardar para un partido.
     *
     * @param players  jugadores convocados con su posición y condición de titular
     * @param rating   valoración del entrenador sobre la alineación (1 a 5), opcional
     * @param note     observación del entrenador, opcional
     */
    public record SaveLineupRequest(
            @NotEmpty(message = "La alineación necesita al menos un jugador")
            @Valid List<PlayerOnField> players,
            @Min(value = 1, message = "La valoración va de 1 a 5")
            @Max(value = 5, message = "La valoración va de 1 a 5")
            Short rating,
            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String note
    ) {}
}
