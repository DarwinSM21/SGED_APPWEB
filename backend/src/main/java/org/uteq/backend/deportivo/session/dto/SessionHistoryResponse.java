package org.uteq.backend.deportivo.session.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SessionHistoryResponse(
        Long sessionId,
        String category,
        String coach,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String field,
        String status,
        boolean hasEvaluation,
        String evaluationStatus,
        Summary summary,
        List<AttendanceRow> attendances
) {
    public record Summary(
            int calledUp,
            int present,
            int late,
            int absentees,
            int excused,
            int withoutRecord
    ) {}

    public record AttendanceRow(
            Long studentId,
            String fullName,
            String position,
            String status,
            LocalTime checkInTime,
            String method,
            String note
    ) {}
}
