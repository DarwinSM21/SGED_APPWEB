package org.uteq.backend.academico.student.dto;

import java.util.List;

public final class MyTeamDtos {
    private MyTeamDtos() {}

    public record CategoryDetailResponse(
            String name,
            Integer minAge,
            Integer maxAge,
            String description
    ) {}

    /**
     * @param nombre nombre de la posición
     * @param abreviatura abreviatura de la posición (ej. para el marcador del equipo)
     */
    public record PositionResponse(String name, String abbreviation) {}

    /**
     * @param nombre nombre del entrenador asignado
     * @param especialidad especialidad del entrenador
     */
    public record AssignedCoachResponse(String name, String specialty) {}

    /**
     * @param idEstudiante identificador del compañero de equipo
     * @param nombre nombre del compañero de equipo
     * @param posicion posición del compañero, o {@code null} si no tiene una asignada
     */
    public record TeammateResponse(Long studentId, String name, String position) {}

    public record MyTeamResponse(
            CategoryDetailResponse category,
            PositionResponse position,
            AssignedCoachResponse coach,
            List<TeammateResponse> teammates
    ) {}
}
