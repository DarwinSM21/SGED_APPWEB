package org.uteq.backend.deportivo.sesion.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Que paso en una sesion que ya ocurrio: quien estuvo y quien no.
 *
 * <p>No incluye formacion. Una alineacion es la decision de con quien se sale
 * a jugar un partido, y desde V22 vive en /api/partidos; lo que un
 * entrenamiento deja registrado es asistencia y evaluacion.
 */
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
        /** Estado de la evaluación: BORRADOR, FINALIZADA o null si no se abrió. */
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
            /** Sin registro de asistencia: nadie pasó lista por ellos. */
            int sinRegistro
    ) {}

    public record FilaAsistencia(
            Long idEstudiante,
            String nombreCompleto,
            String posicion,
            /** PRESENTE | TARDE | AUSENTE | JUSTIFICADO | SIN_REGISTRO */
            String estado,
            /**
             * Hora medida por el QR. Vacía cuando la marcó el entrenador a
             * mano: él afirma que el chico estuvo, no a qué hora entró.
             */
            LocalTime horaEntrada,
            /** QR | MANUAL */
            String metodo,
            String observacion
    ) {}
}
