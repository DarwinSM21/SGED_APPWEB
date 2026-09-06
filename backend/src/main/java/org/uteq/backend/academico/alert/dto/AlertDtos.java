package org.uteq.backend.academico.alert.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AlertDtos {
    private AlertDtos() {}

    public record StudentAtRiskResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            boolean mensualidadPendiente,
            boolean asistenciaBaja,
            BigDecimal porcentajeAsistencia,
            boolean lesionActiva,
            int totalAlertas
    ) {}

    public record AlertsPanelResponse(
            int anio,
            int mes,
            int umbralAsistencia,
            long estudiantesActivos,
            long conMensualidadPendiente,
            long conAsistenciaBaja,
            long conLesionActiva,
            long totalEnRiesgo,
            List<StudentAtRiskResponse> estudiantes
    ) {}
}
