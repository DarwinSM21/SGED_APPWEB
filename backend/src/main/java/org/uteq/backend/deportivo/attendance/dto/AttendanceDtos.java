package org.uteq.backend.deportivo.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Contenedor de los DTO de asistencia para autoconsulta y reportes. */
public final class AttendanceDtos {
    private AttendanceDtos() {}

    /**
     * Un registro de asistencia del estudiante.
     *
     * @param attendanceId  identificador del registro
     * @param date          fecha de la sesión
     * @param category      categoría deportiva de la sesión
     * @param checkInTime   hora de marcación, o {@code null} si no asistió
     * @param status        estado de la asistencia (present/late/absent/excused)
     */
    public record AttendanceResponse(
            Long attendanceId,
            LocalDate date,
            String category,
            LocalTime checkInTime,
            String status
    ) {}

    /**
     * Historial de asistencia del estudiante para autoconsulta.
     *
     * @param attendances           registros de asistencia del periodo
     * @param percentageLast30Days  porcentaje de asistencia de los últimos 30 días
     */
    public record MyHistoryResponse(
            List<AttendanceResponse> attendances,
            BigDecimal percentageLast30Days
    ) {}

    /**
     * Resumen de asistencia de un día para el mapa de calor.
     *
     * @param date        fecha del día
     * @param present     cantidad de presentes ese día
     * @param expected    cantidad de estudiantes activos esperados
     * @param percentage  porcentaje de asistencia del día
     */
    public record AttendanceDayResponse(
            LocalDate date,
            long present,
            long expected,
            BigDecimal percentage
    ) {}

    /**
     * Mapa de calor de asistencia de un rango de fechas.
     *
     * @param from      fecha inicial del rango
     * @param to        fecha final del rango
     * @param days      resumen día a día
     * @param average   promedio de asistencia del rango
     * @param bestDay   día con mejor asistencia, o {@code null} si no hay datos
     * @param worstDay  día con peor asistencia, o {@code null} si no hay datos
     */
    public record AttendanceMapResponse(
            LocalDate from,
            LocalDate to,
            List<AttendanceDayResponse> days,
            BigDecimal average,
            AttendanceDayResponse bestDay,
            AttendanceDayResponse worstDay
    ) {}
}
