package org.uteq.backend.estudiante.dto;

import jakarta.validation.constraints.NotBlank;

public record EstudianteRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        String categoria
) {}