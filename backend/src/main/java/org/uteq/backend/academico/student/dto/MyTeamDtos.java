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

    public record MyTeamResponse(
            CategoryDetailResponse category,
            PositionResponse position,
            AssignedCoachResponse coach,
            List<TeammateResponse> teammates
    ) {}
}
