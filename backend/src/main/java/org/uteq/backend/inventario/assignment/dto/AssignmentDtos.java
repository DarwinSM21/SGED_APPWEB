package org.uteq.backend.inventario.assignment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.inventario.assignment.entity.Assignment.AssignmentStatus;
import org.uteq.backend.inventario.assignment.entity.Assignment.RecipientType;

import java.time.Instant;
import java.time.LocalDate;

public final class AssignmentDtos {
    private AssignmentDtos() {}

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

    public record ReturnRequest(
            @NotNull(message = "El estado de devolución es obligatorio")
            AssignmentStatus status,
            @Size(max = 255, message = "Las observaciones no pueden superar los 255 caracteres")
            String notes
    ) {}

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
