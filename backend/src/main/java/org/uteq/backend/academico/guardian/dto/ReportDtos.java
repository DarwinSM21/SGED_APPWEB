package org.uteq.backend.academico.guardian.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() {}

    public record StudentSummaryResponse(
            Long studentId,
            String fullName,
            String category
    ) {}

    /**
     * @param criterio nombre del criterio de evaluación
     * @param promedio promedio de ese criterio en el período del reporte, o {@code null} sin evaluaciones
     */
    public record CriterionAverageResponse(String criterion, Double average) {}

    public record InjurySummaryResponse(
            Long injuryId,
            String description,
            LocalDate injuryDate,
            LocalDate estimatedReturnDate,
            LocalDate dischargeDate,
            boolean active
    ) {}

    public record StudentReportResponse(
            Long studentId,
            String fullName,
            String category,
            List<CriterionAverageResponse> averagesByCriterion,
            List<InjurySummaryResponse> injuryHistory,
            BigDecimal attendancePercentage
    ) {}

    public record ReportCommentResponse(
            String comment,
            boolean available,
            String reason
    ) {}
}
