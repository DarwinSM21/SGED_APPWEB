package org.uteq.backend.deportivo.injury.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class InjuryDtos {
    private InjuryDtos() {}

    public record RegisterInjuryRequest(
            @NotNull Long studentId,
            Long coachId,
            @NotBlank @Size(max = 1000) String description,
            LocalDate injuryDate,
            LocalDate estimatedReturnDate
    ) {}

    public record DischargeRequest(LocalDate dischargeDate) {}

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
