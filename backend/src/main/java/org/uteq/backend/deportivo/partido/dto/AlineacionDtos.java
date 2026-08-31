package org.uteq.backend.deportivo.partido.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AlineacionDtos {
    public record JugadorEnCancha(
            @NotNull Long idEstudiante,
            Long idPosicion,
            @NotNull Boolean titular
    ) {}

    public record GuardarAlineacionRequest(
            @NotEmpty(message = "La alineación necesita al menos un jugador")
            @Valid List<JugadorEnCancha> jugadores,
            @Min(value = 1, message = "La valoración va de 1 a 5")
            @Max(value = 5, message = "La valoración va de 1 a 5")
            Short valoracion,
            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String observacion
    ) {}
}
