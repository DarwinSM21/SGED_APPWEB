package org.uteq.backend.academico.estudiante.dto;

/**
 * Body de PUT /api/estudiantes/{id}/posicion. idPosicion null quita la
 * posicion asignada (a diferencia de EstudianteRequest, este endpoint no
 * toca ningun otro campo de la ficha -- lo usa tambien ENTRENADOR desde
 * evaluacion diaria, que no tiene por que poder editar categoria, codigo
 * o fecha de ingreso).
 */
public record ActualizarPosicionRequest(Long idPosicion) {}
