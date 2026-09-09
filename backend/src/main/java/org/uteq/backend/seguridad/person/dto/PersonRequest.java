package org.uteq.backend.seguridad.person.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.uteq.backend.common.validation.Cedula;

import java.time.LocalDate;

public record PersonRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @Cedula
        String cedula,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Debe ingresar un correo electrónico válido")
        @Size(max = 200, message = "El correo no puede superar los 200 caracteres")
        String correo,

        @Size(max = 15, message = "El teléfono no puede superar los 15 caracteres")
        String telefono,

        String foto,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate fechaNacimiento
) {}
