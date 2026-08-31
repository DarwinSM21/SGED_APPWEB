package org.uteq.backend.deportivo.evaluacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class EvaluacionDtos {
    private EvaluacionDtos() {}

    public record JugadorEvaluableResponse(
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

    public record EvaluacionSesionResponse(
            Long idEvaluacion,
            Long idSesion,
            LocalDate fecha,
            String categoria,
            String estado,
            List<CriterioResponse> criterios,
            List<JugadorEvaluableResponse> jugadores,
            String observacionGeneral
    ) {}

    public record CriterioResponse(
            Long idCriterio,
            String nombre,
            String descripcion,
            Short puntajeMaximo
    ) {}

    public record GuardarJugadorRequest(
            @NotNull Long idEstudiante,
            Long idPosicionJugada,
            @NotNull List<PuntajeCriterioRequest> puntajes
    ) {}

    public record PuntajeCriterioRequest(
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
