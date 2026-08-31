package org.uteq.backend.deportivo.sesion.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SesionHistorialResponse(
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
        Resumen resumen,
        List<FilaAsistencia> asistencias
) {
    public record Resumen(
            int convocados,
            int presentes,
            int tarde,
            int ausentes,
            int justificados,
            int sinRegistro
    ) {}

    public record FilaAsistencia(
            Long idEstudiante,
            String nombreCompleto,
            String posicion,
            String estado,
            LocalTime horaEntrada,
            String metodo,
            String observacion
    ) {}
}
