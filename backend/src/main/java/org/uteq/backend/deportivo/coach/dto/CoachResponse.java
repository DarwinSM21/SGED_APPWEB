package org.uteq.backend.deportivo.coach.dto;

import java.time.OffsetDateTime;

public record CoachResponse(
        Long idEntrenador,
        Long idPersona,
        String nombre,
        String apellido,
        String cedula,
        String correo,
        String telefono,
        Long idUsuario,
        String username,
        Long idEspecialidad,
        String nombreEspecialidad,
        Short experienciaAnios,
        String certificacion,
        Boolean activo,
        OffsetDateTime createdAt
) {}