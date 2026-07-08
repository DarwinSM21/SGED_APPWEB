package org.uteq.backend.estudiante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Validación estricta de entrada (OWASP A03, Bloque C.2): los campos solo
 * admiten letras, espacios y guiones. Un payload como ' OR '1'='1 se
 * rechaza con 422 ProblemDetails antes de tocar la capa de datos.
 * La defensa primaria sigue siendo la parametrización estricta
 * (Spring Data + funciones SQL con parámetros nombrados, Bloque A.2).
 */
public record EstudianteRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        @Pattern(regexp = "^[\\p{L}][\\p{L} .-]*$",
                message = "El nombre solo admite letras, espacios, puntos y guiones")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100)
        @Pattern(regexp = "^[\\p{L}][\\p{L} .-]*$",
                message = "El apellido solo admite letras, espacios, puntos y guiones")
        String apellido,

        @Size(max = 25)
        @Pattern(regexp = "^[A-Za-z0-9 -]*$",
                message = "La categoría solo admite letras, números y guiones")
        String categoria
) {}
