package org.uteq.backend.academico.estudiante.dto;

import java.util.List;

/**
 * Lo que un ESTUDIANTE ve sobre su propio equipo: su categoria, su
 * posicion nominal, quien es su entrenador (el de la sesion programada
 * mas proxima de su categoria) y sus companeros.
 *
 * <p>Companeros lleva deliberadamente solo nombre y posicion -son menores
 * de edad, sin datos de contacto ni promedios entre compañeros-.
 */
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
            /** null si el estudiante no tiene posicion nominal asignada todavia. */
            PosicionResponse posicion,
            /** null si su categoria no tiene ninguna sesion programada a futuro. */
            EntrenadorAsignadoResponse entrenador,
            List<CompaneroResponse> companeros
    ) {}
}
