package org.uteq.backend.deportivo.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class EvaluationDtos {
    private EvaluationDtos() {}

    public record EvaluablePlayerResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            Long idPosicion,
            String posicion,
            String estadoAsistencia,
            Map<String, BigDecimal> puntajes,
            boolean precargado,
            boolean lesionado,
            Long idLesion,
            boolean puedeEvaluarse,
            String motivoBloqueo
    ) {}

    public record SessionEvaluationResponse(
            Long idEvaluacion,
            Long idSesion,
            LocalDate fecha,
            String categoria,
            String estado,
            List<CriterionResponse> criterios,
            List<EvaluablePlayerResponse> jugadores,
            String observacionGeneral
    ) {}

    public record CriterionResponse(
            Long idCriterio,
            String nombre,
            String descripcion,
            Short puntajeMaximo
    ) {}

    public record SavePlayerRequest(
            @NotNull Long idEstudiante,
            Long idPosicionJugada,
            @NotNull List<CriterionScoreRequest> puntajes
    ) {}

    public record CriterionScoreRequest(
            @NotNull Long idCriterio,
            @NotNull
            @DecimalMin(value = "0.0", message = "El puntaje no puede ser negativo")
            @DecimalMax(value = "10.0", message = "El puntaje no puede superar 10")
            BigDecimal puntaje
    ) {}

    public record FeedbackResponse(
            Long idEstudiante,
            String texto,
            boolean generadoPorIa,
            String motivoNoDisponible
    ) {}
}
