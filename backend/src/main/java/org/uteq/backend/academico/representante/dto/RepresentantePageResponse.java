package org.uteq.backend.academico.representante.dto;

import java.util.List;

public record RepresentantePageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}
