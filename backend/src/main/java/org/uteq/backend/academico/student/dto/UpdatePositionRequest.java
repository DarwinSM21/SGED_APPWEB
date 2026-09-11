package org.uteq.backend.academico.student.dto;

/**
 * @param idPosicion identificador de la nueva posición, o {@code null} para dejar al estudiante sin posición
 */
public record UpdatePositionRequest(Long idPosicion) {}
