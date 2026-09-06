package org.uteq.backend.academico.guardian.dto;

import jakarta.validation.constraints.Size;

public record LinkRequest(
        @Size(max = 50, message = "La relación no puede superar los 50 caracteres")
        String relacion,
        Boolean contactoPrincipal
) {}
