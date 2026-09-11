package org.uteq.backend.academico.student.dto;

import java.util.List;

public final class MyTeamDtos {
    private MyTeamDtos() {}

    public record CategoryDetailResponse(
            String nombre,
            Integer edadMin,
            Integer edadMax,
            String descripcion
    ) {}

    /**
     * @param nombre nombre de la posición
     * @param abreviatura abreviatura de la posición (ej. para el marcador del equipo)
     */
    public record PositionResponse(String nombre, String abreviatura) {}

    /**
     * @param nombre nombre del entrenador asignado
     * @param especialidad especialidad del entrenador
     */
    public record AssignedCoachResponse(String nombre, String especialidad) {}

    /**
     * @param idEstudiante identificador del compañero de equipo
     * @param nombre nombre del compañero de equipo
     * @param posicion posición del compañero, o {@code null} si no tiene una asignada
     */
    public record TeammateResponse(Long idEstudiante, String nombre, String posicion) {}

    public record MyTeamResponse(
            CategoryDetailResponse categoria,
            PositionResponse posicion,
            AssignedCoachResponse entrenador,
            List<TeammateResponse> companeros
    ) {}
}
