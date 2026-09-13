package org.uteq.backend.deportivo.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AttendanceDtos {
    private AttendanceDtos() {}

    public record AttendanceResponse(
            Long attendanceId,
            LocalDate date,
            String category,
            LocalTime checkInTime,
            String status
    ) {}

    public record MyHistoryResponse(
            List<AttendanceResponse> attendances,
            BigDecimal percentageLast30Days
    ) {}

    public record AttendanceDayResponse(
            LocalDate date,
            long present,
            long expected,
            BigDecimal percentage
    ) {}

    public record AttendanceMapResponse(
            LocalDate from,
            LocalDate to,
            List<AttendanceDayResponse> days,
            BigDecimal average,
            AttendanceDayResponse bestDay,
            AttendanceDayResponse worstDay
    ) {}
}
