package org.uteq.backend.deportivo.injury.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Contenedor de los DTO del dominio de lesiones. */
public final class InjuryDtos {
    private InjuryDtos() {}

    /** Datos para registrar una lesión nueva. */
    public record RegisterInjuryRequest(
            @NotNull Long studentId,
            Long coachId,
            @NotBlank @Size(max = 1000) String description,
            LocalDate injuryDate,
            LocalDate estimatedReturnDate
    ) {}

    /** Fecha de alta médica que cierra una lesión activa. */
    public record DischargeRequest(LocalDate dischargeDate) {}

    /** Vista de una lesión para el cliente. */
    public record InjuryResponse(
            Long injuryId,
            Long studentId,
            String student,
            String description,
            LocalDate injuryDate,
            LocalDate estimatedReturnDate,
            LocalDate dischargeDate,
            boolean active
    ) {}
}
