package org.uteq.backend.deportivo.coach.dto;

import java.util.List;

/**
 * Página genérica de resultados para los listados del dominio de entrenadores.
 *
 * @param <T>            tipo de elemento de la página
 * @param content        elementos de esta página
 * @param pageNumber     número de página (base 0)
 * @param pageSize       tamaño de página solicitado
 * @param totalElements  total de elementos que cumplen el filtro
 * @param totalPages     total de páginas disponibles
 */
public record CoachPageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}