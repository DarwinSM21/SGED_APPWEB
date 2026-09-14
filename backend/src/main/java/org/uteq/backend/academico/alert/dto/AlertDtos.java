package org.uteq.backend.academico.alert.dto;

import java.math.BigDecimal;
import java.util.List;

/** Contenedor de los DTO del panel de alertas de riesgo. */
public final class AlertDtos {
    private AlertDtos() {}

    /**
     * Un estudiante con una o más señales de riesgo activas.
     *
     * @param studentId               identificador del estudiante
     * @param fullName                nombre completo para mostrar
     * @param category                categoría deportiva actual
     * @param pendingMembershipFee    {@code true} si tiene una membresía vencida sin pagar
     * @param lowAttendance           {@code true} si su asistencia está bajo el umbral configurado
     * @param attendancePercentage    porcentaje de asistencia del periodo evaluado
     * @param activeInjury            {@code true} si tiene una lesión sin dar de alta
     * @param totalAlerts             cantidad de señales de riesgo activas para este estudiante
     */
    public record StudentAtRiskResponse(
            Long studentId,
            String fullName,
            String category,
            boolean pendingMembershipFee,
            boolean lowAttendance,
            BigDecimal attendancePercentage,
            boolean activeInjury,
            int totalAlerts
    ) {}

    /**
     * Resumen del panel de alertas para un mes calendario.
     *
     * @param year                  año evaluado
     * @param month                 mes evaluado (1-12)
     * @param attendanceThreshold   porcentaje mínimo de asistencia usado para marcar riesgo
     * @param activeStudents        total de estudiantes activos en el periodo
     * @param withPendingMembership cantidad con membresía vencida sin pagar
     * @param withLowAttendance     cantidad bajo el umbral de asistencia
     * @param withActiveInjury      cantidad con lesión sin dar de alta
     * @param totalAtRisk           cantidad de estudiantes con al menos una señal de riesgo
     * @param students              detalle de cada estudiante con riesgo
     */
    public record AlertsPanelResponse(
            int year,
            int month,
            int attendanceThreshold,
            long activeStudents,
            long withPendingMembership,
            long withLowAttendance,
            long withActiveInjury,
            long totalAtRisk,
            List<StudentAtRiskResponse> students
    ) {}
}
