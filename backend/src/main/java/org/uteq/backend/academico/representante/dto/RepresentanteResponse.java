package org.uteq.backend.academico.representante.dto;

import java.time.Instant;
import java.util.List;

public record RepresentanteResponse(
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
        List<EstudianteVinculadoResponse> representados
) {
    public record EstudianteVinculadoResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            String relacion,
            Boolean contactoPrincipal
    ) {}
}
