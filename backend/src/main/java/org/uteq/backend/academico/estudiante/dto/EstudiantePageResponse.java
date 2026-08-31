package org.uteq.backend.academico.estudiante.dto;

import java.io.Serializable;
import java.util.List;

public record EstudiantePageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {
}
