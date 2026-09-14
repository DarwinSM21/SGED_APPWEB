package org.uteq.backend.deportivo.specialty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar una especialidad de entrenador.
 *
 * @param name nombre de la especialidad
 */
public record SpecialtyRequest(
        @NotBlank(message = "El nombre de la especialidad es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name
) {}
