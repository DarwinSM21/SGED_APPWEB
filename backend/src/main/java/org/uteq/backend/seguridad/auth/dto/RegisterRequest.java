package org.uteq.backend.seguridad.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String nombre,
        @NotBlank @Size(min = 2, max = 100) String apellido,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "La cedula debe tener 10 digitos") String cedula,
        @NotBlank @Email @Size(max = 200) String correo,
        @NotNull @Past @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaNacimiento,
        @NotBlank @Email @Size(max = 50) String username,
        @NotBlank @Size(min = 6) String password,
        @NotBlank(message = "El rol es obligatorio") String rol
) {}
