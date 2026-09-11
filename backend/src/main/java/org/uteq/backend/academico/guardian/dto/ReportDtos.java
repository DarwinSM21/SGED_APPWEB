package org.uteq.backend.academico.guardian.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() {}

    public record StudentSummaryResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria
    ) {}

    /**
     * @param criterio nombre del criterio de evaluación
     * @param promedio promedio de ese criterio en el período del reporte, o {@code null} sin evaluaciones
     */
    public record CriterionAverageResponse(String criterio, Double promedio) {}

    public record InjurySummaryResponse(
            Long idLesion,
            String descripcion,
            LocalDate fechaLesion,
            LocalDate fechaEstimadaRetorno,
            LocalDate fechaAlta,
            boolean activa
    ) {}

    public record StudentReportResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            List<CriterionAverageResponse> promediosPorCriterio,
            List<InjurySummaryResponse> historialLesiones,
            BigDecimal porcentajeAsistencia
    ) {}

    public record ReportCommentResponse(
            String comentario,
            boolean disponible,
            String motivo
    ) {}
}
