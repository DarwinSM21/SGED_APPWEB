package org.uteq.backend.academico.alert.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AlertDtos {
    private AlertDtos() {}

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
