package org.uteq.backend.academico.guardian.dto;

import jakarta.validation.constraints.Size;

/**
 * Datos para vincular un estudiante a un representante existente.
 *
 * @param relationship    relación del representante con este estudiante
 * @param primaryContact  {@code true} si debe quedar como contacto principal del estudiante
 */
public record LinkRequest(
        @Size(max = 50, message = "La relación no puede superar los 50 caracteres")
        String relationship,
        Boolean primaryContact
) {}
