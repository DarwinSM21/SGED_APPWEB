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
}
