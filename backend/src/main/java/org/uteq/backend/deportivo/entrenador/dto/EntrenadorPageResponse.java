package org.uteq.backend.deportivo.entrenador.dto;

import java.util.List;

public record EntrenadorPageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}