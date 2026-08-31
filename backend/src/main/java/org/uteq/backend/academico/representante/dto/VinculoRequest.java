package org.uteq.backend.academico.representante.dto;

import jakarta.validation.constraints.Size;

public record VinculoRequest(
        @Size(max = 50, message = "La relación no puede superar los 50 caracteres")
        String relacion,
        Boolean contactoPrincipal
) {}
