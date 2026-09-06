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
            Long idArticulo,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            Integer cantidad,
            @NotNull(message = "El tipo de destinatario es obligatorio")
            RecipientType tipoDestinatario,
            Long idEstudiante,
            Long idEntrenador,
            LocalDate fechaDevolucionEsperada,
            @Size(max = 255, message = "Las observaciones no pueden superar los 255 caracteres")
            String observaciones
    ) {}

    public record ReturnRequest(
            @NotNull(message = "El estado de devolución es obligatorio")
            AssignmentStatus estado,
            @Size(max = 255, message = "Las observaciones no pueden superar los 255 caracteres")
            String observaciones
    ) {}

    public record AssignmentResponse(
            Long idAsignacion,
            Long idArticulo,
            String articulo,
            Integer cantidad,
            RecipientType tipoDestinatario,
            Long idEstudiante,
            String estudiante,
            Long idEntrenador,
            String entrenador,
            LocalDate fechaAsignacion,
            LocalDate fechaDevolucionEsperada,
            LocalDate fechaDevolucionReal,
            AssignmentStatus estado,
            String registradoPor,
            String observaciones,
            Instant createdAt
    ) {}
}
