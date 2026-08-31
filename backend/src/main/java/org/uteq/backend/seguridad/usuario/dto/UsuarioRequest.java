package org.uteq.backend.seguridad.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long idPersona,
        @NotNull(message = "El ID de estado general es obligatorio") Long idEstadoGeneral,
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres") String username,
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password,
        String rol
) {}
