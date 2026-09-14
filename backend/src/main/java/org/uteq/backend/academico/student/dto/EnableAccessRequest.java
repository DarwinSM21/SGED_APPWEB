package org.uteq.backend.academico.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para habilitar el acceso de autoconsulta de un estudiante.
 *
 * @param username nombre de usuario a crear (debe ser un correo)
 * @param password contraseña elegida; la complejidad la valida {@code PasswordPolicy} (RNF-14)
 */
public record EnableAccessRequest(
        @NotBlank @Email @Size(max = 50) String username,
        // La complejidad la valida PasswordPolicy (RNF-14) en StudentAccessService.
        @NotBlank String password
) {}
