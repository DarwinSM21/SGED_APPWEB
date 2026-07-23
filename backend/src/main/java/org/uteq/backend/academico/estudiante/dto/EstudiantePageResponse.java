package org.uteq.backend.academico.estudiante.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Envoltorio de paginación serializable. Se usa en lugar de Page<T> porque
 * PageImpl no se serializa/deserializa de forma estable en Redis con Jackson.
 */
public record EstudiantePageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {
}
