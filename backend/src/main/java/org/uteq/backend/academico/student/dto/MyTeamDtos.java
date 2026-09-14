package org.uteq.backend.academico.student.dto;

import java.util.List;

/** Contenedor de los DTO de la autoconsulta de "mi equipo" del estudiante. */
public final class MyTeamDtos {
    private MyTeamDtos() {}

    /**
     * Detalle de la categoría deportiva del estudiante.
     *
     * @param name        nombre de la categoría
     * @param minAge      edad mínima permitida
     * @param maxAge      edad máxima permitida
     * @param description descripción de la categoría
     */
    public record CategoryDetailResponse(
            String name,
            Integer minAge,
            Integer maxAge,
            String description
    ) {}

    /**
     * @param name nombre de la posición
     * @param abbreviation abreviatura de la posición (ej. para el marcador del equipo)
     */
    public record PositionResponse(String name, String abbreviation) {}

    /**
     * @param name nombre del entrenador asignado
     * @param specialty especialidad del entrenador
     */
    public record AssignedCoachResponse(String name, String specialty) {}

    /**
     * @param studentId identificador del compañero de equipo
     * @param name nombre del compañero de equipo
     * @param position posición del compañero, o {@code null} si no tiene una asignada
     */
    public record TeammateResponse(Long studentId, String name, String position) {}

    /**
     * Vista de "mi equipo" para la autoconsulta del estudiante.
     *
     * @param category   categoría deportiva del estudiante
     * @param position   posición del estudiante, o {@code null} si no tiene una asignada
     * @param coach      entrenador asignado, o {@code null} si no tiene
     * @param teammates  compañeros de la misma categoría
     */
    public record MyTeamResponse(
            CategoryDetailResponse category,
            PositionResponse position,
            AssignedCoachResponse coach,
            List<TeammateResponse> teammates
    ) {}
}
