package org.uteq.backend.academico.estudianteRepresentante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EstudianteRepresentanteRequest(

        @NotNull(message = "El ID del estudiante es obligatorio")
        Long idEstudiante,

        @NotNull(message = "El ID del representante es obligatorio")
        Long idRepresentante,

        @NotBlank(message = "La relación es obligatoria")
        @Size(max = 50, message = "La relación no debe superar los 50 caracteres")
        String relacion,

        Boolean contactoPrincipal
) {}
