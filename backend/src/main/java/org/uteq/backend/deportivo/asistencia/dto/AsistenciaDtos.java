package org.uteq.backend.deportivo.asistencia.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AsistenciaDtos {

    private AsistenciaDtos() {}

    public record AsistenciaResponse(
            Long idAsistencia,
            LocalDate fecha,
            String categoria,
            LocalTime horaEntrada,
            String estado
    ) {}

    public record MiHistorialResponse(
            List<AsistenciaResponse> asistencias,
            BigDecimal porcentajeUltimos30Dias
    ) {}

    /**
     * Un dia del mapa de calor. {@code esperados} es la cantidad de
     * estudiantes activos de las categorias que entrenaron ese dia; sin ese
     * denominador, 12 presentes no dice nada -puede ser excelente o
     * pesimo segun cuantos debian venir-.
     */
    public record DiaAsistenciaResponse(
            LocalDate fecha,
            long presentes,
            long esperados,
            BigDecimal porcentaje
    ) {}

    public record MapaAsistenciaResponse(
            LocalDate desde,
            LocalDate hasta,
            List<DiaAsistenciaResponse> dias,
            /** Promedio sobre los dias que tuvieron entrenamiento, no sobre el calendario. */
            BigDecimal promedio,
            DiaAsistenciaResponse mejorDia,
            DiaAsistenciaResponse peorDia
    ) {}
}
