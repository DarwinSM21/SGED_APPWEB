package org.uteq.backend.academico.guardian.dto;

import java.util.List;

public record GuardianPageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}
