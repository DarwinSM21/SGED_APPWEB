package org.uteq.backend.academico.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnableAccessRequest(
        @NotBlank @Email @Size(max = 50) String username,
        // La complejidad la valida PasswordPolicy (RNF-14) en StudentAccessService.
        @NotBlank String password
) {}
