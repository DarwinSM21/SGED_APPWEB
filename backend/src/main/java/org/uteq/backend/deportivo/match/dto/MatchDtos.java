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
            Long categoryId,
            @NotNull(message = "Indicá la fecha del partido")
            LocalDate date,
            LocalTime time,
            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String note
    ) {}

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

    public record MatchPageResponse(
            List<MatchResponse> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages
    ) {}
}
