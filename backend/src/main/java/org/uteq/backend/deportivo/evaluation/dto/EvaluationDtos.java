package org.uteq.backend.deportivo.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Contenedor de los DTO de evaluación diaria de jugadores. */
public final class EvaluationDtos {
    private EvaluationDtos() {}

    /**
     * Un estudiante disponible para evaluar en una sesión.
     *
     * @param studentId        identificador del estudiante
     * @param fullName         nombre completo
     * @param category         categoría deportiva
     * @param positionId       identificador de la posición asignada en la sesión
     * @param position         nombre de la posición asignada
     * @param attendanceStatus estado de asistencia a la sesión
     * @param scores           puntajes ya guardados por criterio, si los tiene
     * @param preloaded        {@code true} si los puntajes vienen precargados del día anterior
     * @param injured          {@code true} si tiene una lesión activa
     * @param injuryId         identificador de la lesión activa, si tiene
     * @param canBeEvaluated   {@code true} si puede recibir calificación en esta sesión
     * @param blockReason      motivo por el que no puede evaluarse, si aplica
     */
    public record EvaluablePlayerResponse(
            Long studentId,
            String fullName,
            String category,
            Long positionId,
            String position,
            String attendanceStatus,
            Map<String, BigDecimal> scores,
            boolean preloaded,
            boolean injured,
            Long injuryId,
            boolean canBeEvaluated,
            String blockReason
    ) {}

    /**
     * Evaluación diaria completa de una sesión.
     *
     * @param evaluationId  identificador de la evaluación
     * @param sessionId     identificador de la sesión evaluada
     * @param date          fecha de la evaluación
     * @param category      categoría deportiva de la sesión
     * @param status        estado de la evaluación (en curso o finalizada)
     * @param criteria      criterios de evaluación disponibles
     * @param players       jugadores evaluables de la sesión
     * @param generalNote   observación general del entrenador
     */
    public record SessionEvaluationResponse(
            Long evaluationId,
            Long sessionId,
            LocalDate date,
            String category,
            String status,
            List<CriterionResponse> criteria,
            List<EvaluablePlayerResponse> players,
            String generalNote
    ) {}

    /**
     * Un criterio del catálogo de evaluación.
     *
     * @param criterionId  identificador del criterio
     * @param name         nombre del criterio
     * @param description  descripción del criterio
     * @param maxScore     puntaje máximo permitido
     */
    public record CriterionResponse(
            Long criterionId,
            String name,
            String description,
            Short maxScore
    ) {}

    /**
     * Calificación de un jugador a guardar dentro de una evaluación diaria.
     *
     * @param studentId          identificador del estudiante calificado
     * @param lineupPositionId   posición jugada ese día, opcional
     * @param scores             puntajes por criterio
     */
    public record SavePlayerRequest(
            @NotNull Long studentId,
            Long lineupPositionId,
            @NotNull List<CriterionScoreRequest> scores
    ) {}

    /**
     * Puntaje de un jugador en un criterio de evaluación.
     *
     * @param criterionId  identificador del criterio calificado
     * @param score        puntaje asignado (0 a 10)
     */
    public record CriterionScoreRequest(
            @NotNull Long criterionId,
            @NotNull
            @DecimalMin(value = "0.0", message = "El puntaje no puede ser negativo")
            @DecimalMax(value = "10.0", message = "El puntaje no puede superar 10")
            BigDecimal score
    ) {}

    /**
     * Retroalimentación textual generada para el estudiante.
     *
     * @param studentId          identificador del estudiante
     * @param text               texto de retroalimentación, o {@code null} si no está disponible
     * @param aiGenerated        {@code true} si el texto lo generó un proveedor de IA
     * @param unavailableReason  motivo por el que no está disponible, si aplica
     */
    public record FeedbackResponse(
            Long studentId,
            String text,
            boolean aiGenerated,
            String unavailableReason
    ) {}
}
