package org.uteq.backend.academico.guardian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/** Contenedor de los DTO de consentimiento de un representante sobre un estudiante. */
public final class ConsentDtos {

    private ConsentDtos() {}

    /**
     * Solicitud para otorgar un consentimiento.
     *
     * @param guardianId identificador del representante que consiente
     * @param studentId  identificador del estudiante sobre el que aplica
     * @param scope      alcance del consentimiento (p. ej. qué dato o uso cubre)
     */
    public record GrantConsentRequest(
            @NotNull Long guardianId,
            @NotNull Long studentId,
            @NotBlank String scope
    ) {}

    /**
     * Vista de un consentimiento para el cliente.
     *
     * @param consentId             identificador del consentimiento
     * @param guardianId            identificador del representante
     * @param studentId             identificador del estudiante
     * @param scope                 alcance del consentimiento
     * @param grantedAt             fecha y hora en que se otorgó
     * @param registeredByUsername  usuario que registró el consentimiento
     * @param revokedAt             fecha y hora de revocación, o {@code null} si sigue vigente
     * @param vigente               {@code true} si el consentimiento no ha sido revocado
     */
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
