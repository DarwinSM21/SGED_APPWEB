package org.uteq.backend.academico.representante.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RepresentanteRequest(

        @NotNull(message = "El ID de la persona es obligatorio")
        Long idPersona,

        @Size(max = 255, message = "La ocupación no debe superar los 255 caracteres")
        String ocupacion
) {}
