package org.uteq.backend.seguridad.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserAccountRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long idPersona,
        @NotNull(message = "El ID de estado general es obligatorio") Long idEstadoGeneral,
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres") String username,
        // Puede venir en blanco al editar ("no cambiarla"); si trae valor, lo
        // valida PasswordPolicy (RNF-14) en UserAccountService.
        String password,
        String rol
) {}
