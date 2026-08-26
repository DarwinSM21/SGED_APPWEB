package org.uteq.backend.deportivo.partido.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Lo que la pantalla manda al guardar el once de un partido. */
public class AlineacionDtos {

    public record JugadorEnCancha(
            @NotNull Long idEstudiante,
            /** Puesto de ese partido. Puede no ser su posición nominal. */
            Long idPosicion,
            @NotNull Boolean titular
    ) {}

    public record GuardarAlineacionRequest(
            @NotEmpty(message = "La alineación necesita al menos un jugador")
            @Valid List<JugadorEnCancha> jugadores,

            /**
             * De 1 a 5. Opcional: la alineación se arma antes de jugar y se
             * califica después, si es que se califica.
             */
            @Min(value = 1, message = "La valoración va de 1 a 5")
            @Max(value = 5, message = "La valoración va de 1 a 5")
            Short valoracion,

            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String observacion
    ) {}
}
