package org.uteq.backend.seguridad.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos para dar de alta una cuenta.
 *
 * cedula, correo y fechaNacimiento son obligatorios porque la
 * reestructuracion los convirtio en columnas NOT NULL de
 * seguridad.personas. Mientras no estuvieron en este DTO, /api/auth/registro
 * fallaba siempre con 500 al violar la restriccion de la base de datos.
 *
 * rol es opcional (en blanco o ausente = "USER", igual que el comportamiento
 * de siempre): quien llama este endpoint ya es ADMINISTRADOR, asi que puede
 * pedir cualquier rol existente en seguridad.roles. No hay una lista fija
 * aqui a proposito -este proyecto no tiene un enum central de roles, son
 * filas de tabla-; RolRepository.findByNombre es el unico gate, y un
 * nombre que no exista responde 400.
 */
public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String nombre,
        @NotBlank @Size(min = 2, max = 100) String apellido,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "La cedula debe tener 10 digitos") String cedula,
        @NotBlank @Email @Size(max = 200) String correo,
        @NotNull @Past @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaNacimiento,
        @NotBlank @Email @Size(max = 50) String username,
        @NotBlank @Size(min = 6) String password,
        String rol
) {
    public RegisterRequest(String nombre, String apellido, String cedula, String correo,
                            LocalDate fechaNacimiento, String username, String password) {
        this(nombre, apellido, cedula, correo, fechaNacimiento, username, password, null);
    }
}
