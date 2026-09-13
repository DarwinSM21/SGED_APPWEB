package org.uteq.backend.deportivo.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

public class TakeAttendanceDtos {
    public record AttendanceMark(
            @NotNull(message = "El estudiante es obligatorio")
            Long studentId,
            @NotNull(message = "El estado es obligatorio")
            @Pattern(regexp = "PRESENTE|TARDE|AUSENTE|JUSTIFICADO",
                     message = "Estado no válido: use PRESENTE, TARDE, AUSENTE o JUSTIFICADO")
            String status,
            @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
            String note
    ) {}

    public record TakeAttendanceRequest(
            @NotEmpty(message = "Debe enviar al menos un estudiante")
            @Valid
            List<AttendanceMark> marks
    ) {}

    public record RosterRow(
            Long studentId,
            String fullName,
            String status,
            String method,
            LocalTime checkInTime,
            String note
    ) {}

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
