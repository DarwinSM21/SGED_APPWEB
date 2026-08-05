package org.uteq.backend.academico.representante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public final class ConsentimientoDtos {

    private ConsentimientoDtos() {}

    public record OtorgarConsentimientoRequest(
            @NotNull Long idRepresentante,
            @NotNull Long idEstudiante,
            @NotBlank String alcance
    ) {}

    public record ConsentimientoResponse(
            Long idConsentimiento,
            Long idRepresentante,
            Long idEstudiante,
            String alcance,
            OffsetDateTime otorgadoEn,
            String registradoPorUsername,
            OffsetDateTime revocadoEn,
            boolean vigente
    ) {}
}
