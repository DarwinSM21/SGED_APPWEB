package org.uteq.backend.deportivo.session.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Detalle histórico de una sesión de entrenamiento ya ocurrida.
 *
 * @param sessionId         identificador de la sesión
 * @param category          nombre de la categoría
 * @param coach             nombre del entrenador a cargo
 * @param date              fecha de la sesión
 * @param startTime         hora de inicio
 * @param endTime           hora de fin
 * @param field             cancha o lugar
 * @param status            estado de la sesión
 * @param hasEvaluation     {@code true} si tiene una evaluación diaria asociada
 * @param evaluationStatus  estado de la evaluación asociada, si tiene
 * @param summary           resumen numérico de asistencia
 * @param attendances       detalle de asistencia por estudiante
 */
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
    /**
     * Conteo de asistencia de la sesión por estado.
     *
     * @param calledUp       cantidad de estudiantes convocados
     * @param present        cantidad presente
     * @param late           cantidad que llegó tarde
     * @param absentees      cantidad ausente
     * @param excused        cantidad justificada
     * @param withoutRecord  cantidad sin registro de asistencia
     */
    public record Summary(
            int calledUp,
            int present,
            int late,
            int absentees,
            int excused,
            int withoutRecord
    ) {}

    /**
     * Fila de asistencia de un estudiante en la sesión.
     *
     * @param studentId    identificador del estudiante
     * @param fullName     nombre completo
     * @param position     posición del estudiante
     * @param status       estado de asistencia
     * @param checkInTime  hora de marcación, si asistió
     * @param method       método de registro (manual o QR)
     * @param note         observación registrada, si tiene
     */
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
