package org.uteq.backend.academico.representante.dto;

import jakarta.validation.constraints.Size;

/**
 * Datos del vinculo representante-estudiante, no del representante: el
 * parentesco vive aca y no en Representante porque una misma persona puede
 * ser madre de un estudiante y tia de otro.
 */
public record VinculoRequest(
        @Size(max = 50, message = "La relación no puede superar los 50 caracteres")
        String relacion,
        Boolean contactoPrincipal
) {}
