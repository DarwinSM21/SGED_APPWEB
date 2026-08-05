package org.uteq.backend.academico.estudiante.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HabilitarAccesoRequest(
        @NotBlank @Email @Size(max = 50) String username,
        @NotBlank @Size(min = 6) String password
) {}
