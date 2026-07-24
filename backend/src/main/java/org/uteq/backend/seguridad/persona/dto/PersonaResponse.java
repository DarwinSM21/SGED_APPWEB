package org.uteq.backend.seguridad.persona.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PersonaResponse(
        Long idPersona,
        String nombre,
        String apellido,
        String cedula,
        String correo,
        String telefono,
        String foto,
        LocalDate fechaNacimiento,
        Boolean activo,
        Instant createdAt
) {}