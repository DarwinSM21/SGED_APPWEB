package org.uteq.backend.deportivo.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Contenedor de los DTO de partidos. */
public class MatchDtos {
    /**
     * Datos para programar un partido nuevo.
     *
     * @param categoryId  identificador de la categoría que juega
     * @param date        fecha del partido
     * @param time        hora del partido, opcional
     * @param note        observación, opcional
     */
    public record CreateMatchRequest(
            @NotNull(message = "Elegí la categoría que juega")
            Long categoryId,
            @NotNull(message = "Indicá la fecha del partido")
            LocalDate date,
            LocalTime time,
            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String note
    ) {}

    /**
     * Resultado final de un partido.
     *
     * @param goalsFor      goles a favor
     * @param goalsAgainst  goles en contra
     * @param note          observación, opcional
     */
    public record ResultRequest(
            @NotNull(message = "Faltan los goles a favor")
            @Min(value = 0, message = "Los goles no pueden ser negativos")
            @Max(value = 99, message = "Revisá el marcador")
            Short goalsFor,
            @NotNull(message = "Faltan los goles en contra")
            @Min(value = 0, message = "Los goles no pueden ser negativos")
            @Max(value = 99, message = "Revisá el marcador")
            Short goalsAgainst,
            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String note
    ) {}

    /**
     * Vista de un partido para el cliente.
     *
     * @param matchId       identificador del partido
     * @param categoryId    identificador de la categoría
     * @param category      nombre de la categoría
     * @param date          fecha del partido
     * @param time          hora del partido
     * @param goalsFor      goles a favor, si ya se jugó
     * @param goalsAgainst  goles en contra, si ya se jugó
     * @param note          observación del partido
     * @param result        resultado textual (ganado/empatado/perdido/pendiente)
     * @param hasLineup     {@code true} si ya tiene alineación guardada
     * @param starters      cantidad de titulares en la alineación guardada
     * @param closed        {@code true} si el partido está cerrado
     * @param closedAt      fecha y hora de cierre, si está cerrado
     */
    public record MatchResponse(
            Long matchId,
            Long categoryId,
            String category,
            LocalDate date,
            LocalTime time,
            Short goalsFor,
            Short goalsAgainst,
            String note,
            String result,
            boolean hasLineup,
            int starters,
            boolean closed,
            Instant closedAt
    ) {}

    /**
     * Página de partidos.
     *
     * @param content        partidos de esta página
     * @param pageNumber     número de página (base 0)
     * @param pageSize       tamaño de página solicitado
     * @param totalElements  total de partidos que cumplen el filtro
     * @param totalPages     total de páginas disponibles
     */
    public record MatchPageResponse(
            List<MatchResponse> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages
    ) {}
}
