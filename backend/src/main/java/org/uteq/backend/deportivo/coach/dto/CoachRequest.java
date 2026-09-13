package org.uteq.backend.deportivo.coach.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CoachRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long personId,
        @NotNull(message = "El ID de usuario es obligatorio") Long userId,
        Long specialtyId,
        @Min(value = 0, message = "Años de experiencia no pueden ser negativos") Short yearsOfExperience,
        String certification
) {}