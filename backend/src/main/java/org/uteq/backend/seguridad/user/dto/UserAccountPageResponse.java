package org.uteq.backend.seguridad.user.dto;

import java.util.List;

public record UserAccountPageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}
