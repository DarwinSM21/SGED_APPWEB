package org.uteq.backend.estudiante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EstudianteRequest(
        @NotBlank @Size(min = 2, max = 100) String nombre,
        @NotBlank @Size(min = 2, max = 100) String apellido,
        @NotBlank @Pattern(regexp = "^SUB-\\d{1,2}$", message = "categoria debe tener el formato SUB-NN")
        String categoria
) {}
