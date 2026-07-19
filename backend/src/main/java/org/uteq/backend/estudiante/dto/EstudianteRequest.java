package org.uteq.backend.estudiante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstudianteRequest(
        @NotBlank @Size(min = 2, max = 100) String nombre,
        @NotBlank @Size(min = 2, max = 100) String apellido,
        @NotBlank @Size(max = 25) String categoria
) {}
