package org.uteq.backend.seguridad.usuario.dto;

import java.util.List;

public record UsuarioPageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}