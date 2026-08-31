package org.uteq.backend.inventario.asignacion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.inventario.asignacion.entity.Asignacion.EstadoAsignacion;
import org.uteq.backend.inventario.asignacion.entity.Asignacion.TipoDestinatario;

import java.time.Instant;
import java.time.LocalDate;

public final class AsignacionDtos {
    private AsignacionDtos() {}

    public record AsignacionRequest(
            @NotNull(message = "El artículo es obligatorio")
            Long idArticulo,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            Integer cantidad,
            @NotNull(message = "El tipo de destinatario es obligatorio")
            TipoDestinatario tipoDestinatario,
            Long idEstudiante,
            Long idEntrenador,
            LocalDate fechaDevolucionEsperada,
            @Size(max = 255, message = "Las observaciones no pueden superar los 255 caracteres")
            String observaciones
    ) {}

    public record DevolucionRequest(
            @NotNull(message = "El estado de devolución es obligatorio")
            EstadoAsignacion estado,
            @Size(max = 255, message = "Las observaciones no pueden superar los 255 caracteres")
            String observaciones
    ) {}

    public record AsignacionResponse(
            Long idAsignacion,
            Long idArticulo,
            String articulo,
            Integer cantidad,
            TipoDestinatario tipoDestinatario,
            Long idEstudiante,
            String estudiante,
            Long idEntrenador,
            String entrenador,
            LocalDate fechaAsignacion,
            LocalDate fechaDevolucionEsperada,
            LocalDate fechaDevolucionReal,
            EstadoAsignacion estado,
            String registradoPor,
            String observaciones,
            Instant createdAt
    ) {}
}
