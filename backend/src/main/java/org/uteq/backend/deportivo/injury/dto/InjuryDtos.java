package org.uteq.backend.deportivo.injury.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class InjuryDtos {
    private InjuryDtos() {}

    public record RegisterInjuryRequest(
            @NotNull Long idEstudiante,
            Long idEntrenador,
            @NotBlank @Size(max = 1000) String descripcion,
            LocalDate fechaLesion,
            LocalDate fechaEstimadaRetorno
    ) {}

    public record DischargeRequest(LocalDate fechaAlta) {}

    public record InjuryResponse(
            Long idLesion,
            Long idEstudiante,
            String estudiante,
            String descripcion,
            LocalDate fechaLesion,
            LocalDate fechaEstimadaRetorno,
            LocalDate fechaAlta,
            boolean activa
    ) {}
}
