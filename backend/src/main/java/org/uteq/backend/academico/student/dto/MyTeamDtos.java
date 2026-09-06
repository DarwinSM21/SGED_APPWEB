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

    public record PositionResponse(String nombre, String abreviatura) {}

    public record AssignedCoachResponse(String nombre, String especialidad) {}

    public record TeammateResponse(Long idEstudiante, String nombre, String posicion) {}

    public record MyTeamResponse(
            CategoryDetailResponse categoria,
            PositionResponse posicion,
            AssignedCoachResponse entrenador,
            List<TeammateResponse> companeros
    ) {}
}
