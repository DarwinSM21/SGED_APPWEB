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

/**
 * Datos de autorregistro de una persona nueva con su cuenta de acceso.
 *
 * @param name        nombres de la persona
 * @param lastName    apellidos de la persona
 * @param nationalId  cédula de la persona, opcional
 * @param email       correo de contacto
 * @param birthDate   fecha de nacimiento
 * @param username    nombre de usuario a crear
 * @param password    contraseña elegida; la complejidad la valida {@code PasswordPolicy} (RNF-14)
 * @param role        rol solicitado para la cuenta
 */
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
