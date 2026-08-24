package org.uteq.backend.deportivo.evaluacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** La alineacion que el entrenador pone en cancha, distinta de la sugerida. */
public class AlineacionDtos {

    public record JugadorEnCancha(
            @NotNull Long idEstudiante,
            /** Puesto de ese dia. Puede no ser su posicion nominal. */
            Long idPosicion,
            @NotNull Boolean titular
    ) {}

    public record GuardarAlineacionRequest(
            @NotEmpty(message = "La alineación necesita al menos un jugador")
            @Valid List<JugadorEnCancha> jugadores,

            /**
             * De 1 a 5. Opcional: la alineacion se guarda antes de jugar y se
             * califica despues, si es que se califica.
             */
            @Min(value = 1, message = "La valoración va de 1 a 5")
            @Max(value = 5, message = "La valoración va de 1 a 5")
            Short valoracion,

            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String observacion
    ) {}

    public record JugadorAlineadoResponse(
            Long idEstudiante,
            String nombreCompleto,
            String posicion,
            Long idPosicion,
            boolean titular,
            BigDecimal promedioAcumulado
    ) {}

    public record AlineacionResponse(
            Long idSesion,
            String categoria,
            LocalDate fecha,
            /** true = la guardó el entrenador; false = es la sugerencia del sistema. */
            boolean guardada,
            Short valoracion,
            String observacion,
            List<JugadorAlineadoResponse> titulares,
            List<JugadorAlineadoResponse> suplentes,
            /** Quienes asistieron y no están en el once: material para cambios. */
            List<JugadorAlineadoResponse> disponibles
    ) {}

    /** Una fila del historial: con qué once se jugó cada sesión. */
    public record HistorialAlineacionResponse(
            Long idSesion,
            LocalDate fecha,
            String categoria,
            boolean guardada,
            Short valoracion,
            String observacion,
            int titulares
    ) {}
}
