package org.uteq.backend.seguridad.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar una cuenta de usuario.
 *
 * @param personId         identificador de la persona vinculada
 * @param generalStatusId  identificador del estado general de la cuenta
 * @param username         nombre de usuario
 * @param password         contraseña nueva, opcional en edición (vacío = no cambiarla)
 * @param role             rol asignado, opcional
 */
public record UserAccountRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long personId,
        @NotNull(message = "El ID de estado general es obligatorio") Long generalStatusId,
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres") String username,
        // Puede venir en blanco al update ("no cambiarla"); si trae valor, lo
        // valida PasswordPolicy (RNF-14) en UserAccountService.
        String password,
        String role
) {}
