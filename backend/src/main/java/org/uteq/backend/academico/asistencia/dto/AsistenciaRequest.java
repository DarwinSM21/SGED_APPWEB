package org.uteq.backend.academico.asistencia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record AsistenciaRequest(

        @NotNull(message = "El ID de la sesión de entrenamiento es obligatorio")
        Long idSesionEntrenamiento,

        @NotNull(message = "El ID del estudiante es obligatorio")
        Long idEstudiante,

        @NotNull(message = "El ID del estado de asistencia es obligatorio")
        Long idEstadoAsistencia,

        @PastOrPresent(message = "La fecha de registro no puede ser una fecha futura")
        LocalDate fechaRegistro
) {}
