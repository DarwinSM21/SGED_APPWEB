package org.uteq.backend.seguridad.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long idPersona,
        @NotNull(message = "El ID de estado general es obligatorio") Long idEstadoGeneral,
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres") String username,
        /**
         * Opcional al editar: en blanco significa "no cambiar la contraseña
         * actual". Obligatoria al crear (validado a mano en
         * UsuarioService.crear, ya que @NotBlank no puede depender de si es
         * alta o edicion en el mismo DTO). @Size es null-safe: no dispara si
         * el valor es null.
         */
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password,
        /**
         * Opcional. Si viene, se valida contra seguridad.roles y se asigna al
         * crear (mismo criterio que AuthController.registro, pero sin forzar
         * una Persona nueva). Si es null, el usuario queda sin rol -- caso de
         * edicion, que no toca los roles existentes.
         */
        String rol
) {}