package org.uteq.backend.academico.estudiante.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EstudianteRequest(

        // --- Relación con la Persona física ya existente ---
        @NotNull(message = "El ID de la persona es obligatorio")
        Long idPersona,

        // --- Claves Foráneas / Catálogos ---
        @NotNull(message = "El ID de la categoría es obligatorio")
        Long idCategoria,

        @NotNull(message = "El ID del estado general es obligatorio")
        Long idEstadoGeneral,

        // --- Datos Propios del Estudiante ---
        @NotBlank(message = "El código de estudiante es obligatorio")
        @Size(max = 30, message = "El código de estudiante no debe superar los 30 caracteres")
        String codigoEstudiante,

        @NotNull(message = "La fecha de ingreso es obligatoria")
        @PastOrPresent(message = "La fecha de ingreso no puede ser una fecha futura")
        LocalDate fechaIngreso,

        @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
        @Digits(integer = 3, fraction = 2, message = "El peso debe tener máximo 3 enteros y 2 decimales")
        BigDecimal peso,

        @DecimalMin(value = "0.01", message = "La altura debe ser mayor a 0")
        @Digits(integer = 3, fraction = 2, message = "La altura debe tener máximo 3 enteros y 2 decimales")
        BigDecimal altura
) {}