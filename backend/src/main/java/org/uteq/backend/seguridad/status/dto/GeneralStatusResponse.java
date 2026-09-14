package org.uteq.backend.seguridad.status.dto;

/**
 * Un estado general del catálogo administrativo.
 *
 * @param generalStatusId  identificador del estado
 * @param name             nombre del estado
 */
public record GeneralStatusResponse(
    Long generalStatusId,
    String name
) {}
