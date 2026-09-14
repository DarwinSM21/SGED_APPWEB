package org.uteq.backend.deportivo.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/** Contenedor de los DTO para que el entrenador registre la asistencia de una sesión. */
public class TakeAttendanceDtos {
    /**
     * Marca de asistencia de un estudiante dentro de una sesión.
     *
     * @param studentId identificador del estudiante
     * @param status    estado marcado (PRESENT, LATE, ABSENT o EXCUSED)
     * @param note      observación opcional del entrenador
     */
    public record AttendanceMark(
            @NotNull(message = "El estudiante es obligatorio")
            Long studentId,
            @NotNull(message = "El estado es obligatorio")
            @Pattern(regexp = "PRESENT|LATE|ABSENT|EXCUSED",
                     message = "Estado no válido: use PRESENT, LATE, ABSENT o EXCUSED")
            String status,
            @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
            String note
    ) {}

    /**
     * Lote de marcas de asistencia a registrar para una sesión.
     *
     * @param marks marcas de cada estudiante del listado
     */
    public record TakeAttendanceRequest(
            @NotEmpty(message = "Debe enviar al menos un estudiante")
            @Valid
            List<AttendanceMark> marks
    ) {}

    /**
     * Fila del listado de asistencia de una sesión.
     *
     * @param studentId    identificador del estudiante
     * @param fullName     nombre completo del estudiante
     * @param status       estado actual de asistencia, o {@code null} si no se ha marcado
     * @param method       método por el que se registró (manual o QR)
     * @param checkInTime  hora de marcación, si ya se marcó
     * @param note         observación registrada, si tiene
     */
    public record RosterRow(
            Long studentId,
            String fullName,
            String status,
            String method,
            LocalTime checkInTime,
            String note
    ) {}

    /**
     * Listado completo de asistencia de una sesión.
     *
     * @param sessionId          identificador de la sesión
     * @param category           categoría deportiva de la sesión
     * @param date               fecha de la sesión
     * @param startTime          hora de inicio programada
     * @param editable           {@code true} si todavía se puede editar la asistencia
     * @param nonEditableReason  motivo por el que no se puede editar, si aplica
     * @param rows               fila de asistencia por estudiante
     */
    public record RosterResponse(
            Long sessionId,
            String category,
            java.time.LocalDate date,
            LocalTime startTime,
            boolean editable,
            String nonEditableReason,
            List<RosterRow> rows
    ) {}
}
