package org.uteq.backend.academico.student.dto;

/**
 * @param positionId identificador de la nueva posición, o {@code null} para dejar al estudiante sin posición
 */
public record UpdatePositionRequest(Long positionId) {}
