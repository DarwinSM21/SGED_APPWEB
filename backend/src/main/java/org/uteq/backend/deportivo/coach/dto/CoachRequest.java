package org.uteq.backend.deportivo.coach.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CoachRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long idPersona,
        @NotNull(message = "El ID de usuario es obligatorio") Long idUsuario,
        Long idEspecialidad,
        @Min(value = 0, message = "Años de experiencia no pueden ser negativos") Short experienciaAnios,
        String certificacion
) {}