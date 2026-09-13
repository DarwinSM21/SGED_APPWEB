package org.uteq.backend.seguridad.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.uteq.backend.common.validation.NationalId;
// La complejidad de la contraseña la valida PasswordPolicy (RNF-14) en
// AuthService.register, no una anotación de tamaño: la regla es una sola y
// vive en un único lugar.

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Size(min = 2, max = 100) String lastName,
        @NationalId String nationalId,
        @NotBlank @Email @Size(max = 200) String email,
        @NotNull @Past @JsonFormat(pattern = "yyyy-MM-dd") LocalDate birthDate,
        @NotBlank @Email @Size(max = 50) String username,
        @NotBlank String password,
        @NotBlank(message = "El rol es obligatorio") String role
) {}
