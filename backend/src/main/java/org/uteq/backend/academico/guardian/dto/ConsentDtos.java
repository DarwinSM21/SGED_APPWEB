package org.uteq.backend.academico.guardian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public final class ConsentDtos {

    private ConsentDtos() {}

    public record GrantConsentRequest(
            @NotNull Long guardianId,
            @NotNull Long studentId,
            @NotBlank String scope
    ) {}

    public record ConsentResponse(
            Long consentId,
            Long guardianId,
            Long studentId,
            String scope,
            OffsetDateTime grantedAt,
            String registeredByUsername,
            OffsetDateTime revokedAt,
            boolean vigente
    ) {}
}
