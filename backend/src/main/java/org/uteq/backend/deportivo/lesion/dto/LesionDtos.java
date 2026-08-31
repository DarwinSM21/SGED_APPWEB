package org.uteq.backend.deportivo.lesion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class LesionDtos {
    private LesionDtos() {}

    public record RegistrarLesionRequest(
            @NotNull Long idEstudiante,
            Long idEntrenador,
            @NotBlank @Size(max = 1000) String descripcion,
            LocalDate fechaLesion,
            LocalDate fechaEstimadaRetorno
    ) {}

    public record DarDeAltaRequest(LocalDate fechaAlta) {}

    public record LesionResponse(
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
