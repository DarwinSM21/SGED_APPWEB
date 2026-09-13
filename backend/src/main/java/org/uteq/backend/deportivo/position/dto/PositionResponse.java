package org.uteq.backend.deportivo.position.dto;

/** Vista de una posición del catálogo para el cliente. */
public record PositionResponse(Long positionId, String name, String abbreviation) {}
