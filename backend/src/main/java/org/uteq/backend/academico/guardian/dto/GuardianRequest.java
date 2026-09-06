package org.uteq.backend.academico.guardian.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GuardianRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long idPersona,
        @NotNull(message = "El ID de usuario es obligatorio") Long idUsuario,
        String parentesco,
        String telefonoContacto,
        List<Long> idsEstudiantesIniciales
) {}
