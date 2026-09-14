package org.uteq.backend.academico.student.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Página genérica de resultados para los listados del dominio de estudiantes.
 *
 * @param <T>            tipo de elemento de la página
 * @param content        elementos de esta página
 * @param page           número de página (base 0)
 * @param size           tamaño de página solicitado
 * @param totalElements  total de elementos que cumplen el filtro
 * @param totalPages     total de páginas disponibles
 */
public record StudentPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {
}
