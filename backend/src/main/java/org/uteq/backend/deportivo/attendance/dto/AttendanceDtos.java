package org.uteq.backend.deportivo.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AttendanceDtos {
    private AttendanceDtos() {}

    public record AttendanceResponse(
            Long idAsistencia,
            LocalDate fecha,
            String categoria,
            LocalTime horaEntrada,
            String estado
    ) {}

    public record MyHistoryResponse(
            List<AttendanceResponse> asistencias,
            BigDecimal porcentajeUltimos30Dias
    ) {}

    public record AttendanceDayResponse(
            LocalDate fecha,
            long presentes,
            long esperados,
            BigDecimal porcentaje
    ) {}

    public record AttendanceMapResponse(
            LocalDate desde,
            LocalDate hasta,
            List<AttendanceDayResponse> dias,
            BigDecimal promedio,
            AttendanceDayResponse mejorDia,
            AttendanceDayResponse peorDia
    ) {}
}
