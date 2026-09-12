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
            Long idEstudiante,
            @NotNull(message = "El estado es obligatorio")
            @Pattern(regexp = "PRESENTE|TARDE|AUSENTE|JUSTIFICADO",
                     message = "Estado no válido: use PRESENTE, TARDE, AUSENTE o JUSTIFICADO")
            String estado,
            @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
            String observacion
    ) {}

    public record TakeAttendanceRequest(
            @NotEmpty(message = "Debe enviar al menos un estudiante")
            @Valid
            List<AttendanceMark> marcas
    ) {}

    public record RosterRow(
            Long idEstudiante,
            String nombreCompleto,
            String estado,
            String metodo,
            LocalTime horaEntrada,
            String observacion
    ) {}

    public record RosterResponse(
            Long idSesion,
            String categoria,
            java.time.LocalDate fecha,
            LocalTime horaInicio,
            boolean editable,
            String motivoNoEditable,
            List<RosterRow> filas
    ) {}
}
