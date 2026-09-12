package org.uteq.backend.deportivo.coach.dto;

import java.util.List;

public record CoachPageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}