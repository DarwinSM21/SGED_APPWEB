package org.uteq.backend.academico.student.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para crear o editar la ficha de un estudiante.
 *
 * @param personId          identificador de la persona vinculada
 * @param categoryId        identificador de la categoría deportiva
 * @param generalStatusId   identificador del estado general (activo/inactivo/etc.)
 * @param studentCode       código interno del estudiante
 * @param enrollmentDate    fecha de ingreso a la escuela
 * @param weight            peso del estudiante, opcional
 * @param height            altura del estudiante, opcional
 * @param positionId        identificador de la posición de juego, opcional
 */
public record StudentRequest(
        @NotNull(message = "El ID de la persona es obligatorio")
        Long personId,
        @NotNull(message = "El ID de la categoría es obligatorio")
        Long categoryId,
        @NotNull(message = "El ID del estado general es obligatorio")
        Long generalStatusId,
        @NotBlank(message = "El código de estudiante es obligatorio")
        @Size(max = 30, message = "El código de estudiante no debe superar los 30 caracteres")
        String studentCode,
        @NotNull(message = "La fecha de ingreso es obligatoria")
        @PastOrPresent(message = "La fecha de ingreso no puede ser una fecha futura")
        LocalDate enrollmentDate,
        @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
        @Digits(integer = 3, fraction = 2, message = "El peso debe tener máximo 3 enteros y 2 decimales")
        BigDecimal weight,
        @DecimalMin(value = "0.01", message = "La altura debe ser mayor a 0")
        @Digits(integer = 3, fraction = 2, message = "La altura debe tener máximo 3 enteros y 2 decimales")
        BigDecimal height,
        Long positionId
) {}
