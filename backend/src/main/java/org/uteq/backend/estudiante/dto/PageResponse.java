package org.uteq.backend.estudiante.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Envoltorio de paginación serializable. Se usa en lugar de Page<T> porque
 * PageImpl no se serializa/deserializa de forma estable en Redis con Jackson.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {
}
