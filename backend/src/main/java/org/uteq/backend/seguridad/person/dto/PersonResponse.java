package org.uteq.backend.seguridad.person.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PersonResponse(
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
