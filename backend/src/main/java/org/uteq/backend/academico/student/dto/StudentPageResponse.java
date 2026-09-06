package org.uteq.backend.academico.student.dto;

import java.io.Serializable;
import java.util.List;

public record StudentPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {
}
