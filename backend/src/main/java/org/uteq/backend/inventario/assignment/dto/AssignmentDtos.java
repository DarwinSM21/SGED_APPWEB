package org.uteq.backend.inventario.assignment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.inventario.assignment.entity.Assignment.AssignmentStatus;
import org.uteq.backend.inventario.assignment.entity.Assignment.RecipientType;

import java.time.Instant;
import java.time.LocalDate;

/** Contenedor de los DTO de asignación y devolución de equipamiento. */
public final class AssignmentDtos {
    private AssignmentDtos() {}

    /**
     * Datos para registrar una asignación de equipamiento.
     *
     * @param itemId              identificador del artículo asignado
     * @param quantity            cantidad asignada
     * @param recipientType       tipo de destinatario (estudiante o entrenador)
     * @param studentId           identificador del estudiante destinatario, si aplica
     * @param coachId             identificador del entrenador destinatario, si aplica
     * @param expectedReturnDate  fecha esperada de devolución, opcional
     * @param notes               observaciones, opcional
     */
    public record AssignmentRequest(
            @NotNull(message = "El artículo es obligatorio")
            Long itemId,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            Integer quantity,
            @NotNull(message = "El tipo de destinatario es obligatorio")
            RecipientType recipientType,
            Long studentId,
            Long coachId,
            LocalDate expectedReturnDate,
            @Size(max = 255, message = "Las observaciones no pueden superar los 255 caracteres")
            String notes
    ) {}

    /**
     * Datos para registrar la devolución de una asignación.
     *
     * @param status  estado final de la devolución (devuelto o perdido)
     * @param notes   observaciones, opcional
     */
    public record ReturnRequest(
            @NotNull(message = "El estado de devolución es obligatorio")
            AssignmentStatus status,
            @Size(max = 255, message = "Las observaciones no pueden superar los 255 caracteres")
            String notes
    ) {}

    /**
     * Vista de una asignación de equipamiento para el cliente.
     *
     * @param assignmentId        identificador de la asignación
     * @param itemId              identificador del artículo
     * @param item                nombre del artículo
     * @param quantity            cantidad asignada
     * @param recipientType       tipo de destinatario
     * @param studentId           identificador del estudiante destinatario, si aplica
     * @param student             nombre del estudiante destinatario, si aplica
     * @param coachId             identificador del entrenador destinatario, si aplica
     * @param coach               nombre del entrenador destinatario, si aplica
     * @param assignmentDate      fecha en que se asignó
     * @param expectedReturnDate  fecha esperada de devolución
     * @param actualReturnDate    fecha real de devolución, si ya se devolvió
     * @param status              estado actual de la asignación
     * @param registeredBy        usuario que registró la asignación
     * @param notes               observaciones
     * @param createdAt           fecha de creación del registro
     */
    public record AssignmentResponse(
            Long assignmentId,
            Long itemId,
            String item,
            Integer quantity,
            RecipientType recipientType,
            Long studentId,
            String student,
            Long coachId,
            String coach,
            LocalDate assignmentDate,
            LocalDate expectedReturnDate,
            LocalDate actualReturnDate,
            AssignmentStatus status,
            String registeredBy,
            String notes,
            Instant createdAt
    ) {}
}
