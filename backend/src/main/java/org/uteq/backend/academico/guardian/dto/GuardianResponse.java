package org.uteq.backend.academico.guardian.dto;

import java.time.Instant;
import java.util.List;

public record GuardianResponse(
        Long idRepresentante,
        Long idPersona,
        String nombre,
        String apellido,
        String cedula,
        String correo,
        Long idUsuario,
        String username,
        String parentesco,
        String telefonoContacto,
        Boolean activo,
        Instant createdAt,
        List<LinkedStudentResponse> representados
) {
    public record LinkedStudentResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            String relacion,
            Boolean contactoPrincipal
    ) {}
}
