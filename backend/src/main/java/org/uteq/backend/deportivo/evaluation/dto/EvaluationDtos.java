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

    public record CriterionResponse(
            Long criterionId,
            String name,
            String description,
            Short maxScore
    ) {}

    public record SavePlayerRequest(
            @NotNull Long studentId,
            Long lineupPositionId,
            @NotNull List<CriterionScoreRequest> scores
    ) {}

    public record CriterionScoreRequest(
            @NotNull Long criterionId,
            @NotNull
            @DecimalMin(value = "0.0", message = "El puntaje no puede ser negativo")
            @DecimalMax(value = "10.0", message = "El puntaje no puede superar 10")
            BigDecimal score
    ) {}

    public record FeedbackResponse(
            Long studentId,
            String text,
            boolean aiGenerated,
            String unavailableReason
    ) {}
}
