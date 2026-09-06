package org.uteq.backend.academico.guardian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public final class ConsentDtos {

    private ConsentDtos() {}

    public record GrantConsentRequest(
            @NotNull Long idRepresentante,
            @NotNull Long idEstudiante,
            @NotBlank String alcance
    ) {}

    public record ConsentResponse(
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
