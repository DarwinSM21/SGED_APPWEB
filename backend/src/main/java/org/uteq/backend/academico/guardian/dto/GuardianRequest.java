package org.uteq.backend.academico.guardian.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Datos para crear o editar la ficha de un representante.
 *
 * @param personId           identificador de la persona vinculada
 * @param userId             identificador de la cuenta de usuario vinculada
 * @param relationship       relación con el/los estudiantes (p. ej. padre, madre, tutor)
 * @param contactPhone       teléfono de contacto preferido
 * @param initialStudentIds  estudiantes a vincular al crear la ficha
 */
public record GuardianRequest(
        @NotNull(message = "El ID de persona es obligatorio") Long personId,
        @NotNull(message = "El ID de usuario es obligatorio") Long userId,
        String relationship,
        String contactPhone,
        List<Long> initialStudentIds
) {}
