package org.uteq.backend.academico.asistencia.dto;

import java.time.LocalDate;

public record AsistenciaResponse(
        Long idAsistencia,
        Long idSesionEntrenamiento,
        String tituloSesion,
        LocalDate fechaSesion,
        Long idEstudiante,
        String codigoEstudiante,
        String nombreEstudiante,
        String apellidoEstudiante,
        Long idEstadoAsistencia,
        String nombreEstadoAsistencia,
        LocalDate fechaRegistro
) {}
