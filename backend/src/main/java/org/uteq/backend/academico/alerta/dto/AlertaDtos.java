package org.uteq.backend.academico.alerta.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AlertaDtos {
    private AlertaDtos() {}

    public record EstudianteEnRiesgoResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            boolean mensualidadPendiente,
            boolean asistenciaBaja,
            BigDecimal porcentajeAsistencia,
            boolean lesionActiva,
            int totalAlertas
    ) {}

    public record PanelAlertasResponse(
            int anio,
            int mes,
            int umbralAsistencia,
            long estudiantesActivos,
            long conMensualidadPendiente,
            long conAsistenciaBaja,
            long conLesionActiva,
            long totalEnRiesgo,
            List<EstudianteEnRiesgoResponse> estudiantes
    ) {}
}
