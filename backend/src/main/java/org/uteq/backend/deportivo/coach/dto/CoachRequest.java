package org.uteq.backend.deportivo.coach.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear o editar la ficha de un entrenador.
 *
 * @param personId           identificador de la persona vinculada
 * @param userId             identificador de la cuenta de usuario vinculada
 * @param specialtyId        identificador de la especialidad, opcional
 * @param yearsOfExperience  años de experiencia
 * @param certification      certificación o título del entrenador
 */
public record CoachRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long personId,
        @NotNull(message = "El ID de usuario es obligatorio") Long userId,
        Long specialtyId,
        @Min(value = 0, message = "Años de experiencia no pueden ser negativos") Short yearsOfExperience,
        String certification
) {}