package org.uteq.backend.deportivo.entrenador.dto;

import java.time.OffsetDateTime;

public record EntrenadorResponse(
        Long idEntrenador,
        Long idPersona,
        String nombre,
        String apellido,
        String cedula,
        String correo,
        String telefono,
        Long idUsuario,
        String username,
        String especialidad,
        Short experienciaAnios,
        String certificacion,
        Boolean activo,
        OffsetDateTime createdAt
) {}