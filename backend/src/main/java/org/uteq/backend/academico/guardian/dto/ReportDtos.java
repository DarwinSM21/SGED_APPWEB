package org.uteq.backend.academico.guardian.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Contenedor de los DTO de reportes del estudiante para su representante. */
public final class ReportDtos {
    private ReportDtos() {}

    /**
     * Identificación básica de un estudiante dentro de un reporte.
     *
     * @param studentId identificador del estudiante
     * @param fullName  nombre completo del estudiante
     * @param category  categoría deportiva del estudiante
     */
    public record StudentSummaryResponse(
            Long studentId,
            String fullName,
            String category
    ) {}

    /**
     * @param criterion nombre del criterio de evaluación
     * @param average promedio de ese criterio en el período del reporte, o {@code null} sin evaluaciones
     */
    public record CriterionAverageResponse(String criterion, Double average) {}

    /**
     * Resumen de una lesión dentro del historial del reporte.
     *
     * @param injuryId              identificador de la lesión
     * @param description           descripción de la lesión
     * @param injuryDate            fecha en que ocurrió
     * @param estimatedReturnDate   fecha estimada de retorno
     * @param dischargeDate         fecha de alta médica, o {@code null} si sigue activa
     * @param active                {@code true} si la lesión sigue en curso
     */
    public record InjurySummaryResponse(
            Long injuryId,
            String description,
            LocalDate injuryDate,
            LocalDate estimatedReturnDate,
            LocalDate dischargeDate,
            boolean active
    ) {}

    /**
     * Reporte completo de un estudiante para su representante.
     *
     * @param studentId             identificador del estudiante
     * @param fullName              nombre completo del estudiante
     * @param category              categoría deportiva del estudiante
     * @param averagesByCriterion   promedio de evaluación por criterio
     * @param injuryHistory         historial de lesiones del estudiante
     * @param attendancePercentage  porcentaje de asistencia del periodo del reporte
     */
    public record StudentReportResponse(
            Long studentId,
            String fullName,
            String category,
            List<CriterionAverageResponse> averagesByCriterion,
            List<InjurySummaryResponse> injuryHistory,
            BigDecimal attendancePercentage
    ) {}

    /**
     * Comentario libre del entrenador sobre el estudiante, sujeto a consentimiento.
     *
     * @param comment   texto del comentario, o {@code null} si no está disponible
     * @param available {@code true} si el representante tiene consentimiento vigente para verlo
     * @param reason    motivo por el que no está disponible, si aplica
     */
    public record ReportCommentResponse(
            String comment,
            boolean available,
            String reason
    ) {}
}
