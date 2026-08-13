package org.uteq.backend.deportivo.entrenador.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EntrenadorRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long idPersona,
        @NotNull(message = "El ID de usuario es obligatorio") Long idUsuario,
        Long idEspecialidad,
        @Min(value = 0, message = "Años de experiencia no pueden ser negativos") Short experienciaAnios,
        String certificacion
) {}