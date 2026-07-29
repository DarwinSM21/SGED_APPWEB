package org.uteq.backend.seguridad.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String nombre,
        @NotBlank @Size(min = 2, max = 100) String apellido,
<<<<<<< HEAD:backend/src/main/java/org/uteq/backend/seguridad/auth/dto/RegisterRequest.java
        @NotBlank @Email String username,
        @NotBlank @Size(min = 6) String password
=======
        @NotBlank @Pattern(regexp = "^SUB-\\d{1,2}$", message = "categoria debe tener el formato SUB-NN")
        String categoria
>>>>>>> origin/main:backend/src/main/java/org/uteq/backend/estudiante/dto/EstudianteRequest.java
) {}
