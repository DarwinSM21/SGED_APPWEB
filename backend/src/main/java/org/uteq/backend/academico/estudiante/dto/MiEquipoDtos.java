package org.uteq.backend.academico.estudiante.dto;

import java.util.List;

public final class MiEquipoDtos {
    private MiEquipoDtos() {}

    public record CategoriaDetalleResponse(
            String nombre,
            Integer edadMin,
            Integer edadMax,
            String descripcion
    ) {}

    public record PosicionResponse(String nombre, String abreviatura) {}

    public record EntrenadorAsignadoResponse(String nombre, String especialidad) {}

    public record CompaneroResponse(Long idEstudiante, String nombre, String posicion) {}

    public record MiEquipoResponse(
            CategoriaDetalleResponse categoria,
            PosicionResponse posicion,
            EntrenadorAsignadoResponse entrenador,
            List<CompaneroResponse> companeros
    ) {}
}
