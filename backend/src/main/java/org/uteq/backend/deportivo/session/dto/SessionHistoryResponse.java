package org.uteq.backend.deportivo.session.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SessionHistoryResponse(
        Long idSesion,
        String categoria,
        String entrenador,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String campo,
        String estado,
        boolean tieneEvaluacion,
        String estadoEvaluacion,
        Summary resumen,
        List<AttendanceRow> asistencias
) {
    public record Summary(
            int convocados,
            int presentes,
            int tarde,
            int ausentes,
            int justificados,
            int sinRegistro
    ) {}

    public record AttendanceRow(
            Long idEstudiante,
            String nombreCompleto,
            String posicion,
            String estado,
            LocalTime horaEntrada,
            String metodo,
            String observacion
    ) {}
}
