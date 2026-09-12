package org.uteq.backend.deportivo.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class MatchDtos {
    public record CreateMatchRequest(
            @NotNull(message = "Elegí la categoría que juega")
            Long idCategoria,
            @NotNull(message = "Indicá la fecha del partido")
            LocalDate fecha,
            LocalTime hora,
            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String observacion
    ) {}

    public record ResultRequest(
            @NotNull(message = "Faltan los goles a favor")
            @Min(value = 0, message = "Los goles no pueden ser negativos")
            @Max(value = 99, message = "Revisá el marcador")
            Short golesFavor,
            @NotNull(message = "Faltan los goles en contra")
            @Min(value = 0, message = "Los goles no pueden ser negativos")
            @Max(value = 99, message = "Revisá el marcador")
            Short golesContra,
            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String observacion
    ) {}

    public record MatchResponse(
            Long idPartido,
            Long idCategoria,
            String categoria,
            LocalDate fecha,
            LocalTime hora,
            Short golesFavor,
            Short golesContra,
            String observacion,
            String resultado,
            boolean tieneAlineacion,
            int titulares,
            boolean cerrado,
            Instant cerradoEn
    ) {}

    public record MatchPageResponse(
            List<MatchResponse> contenido,
            int pagina,
            int tamano,
            long total,
            int totalPaginas
    ) {}
}
