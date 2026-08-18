package org.uteq.backend.academico.alerta.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AlertaDtos {

    private AlertaDtos() {}

    /**
     * Un estudiante que necesita atencion, con el detalle de por que. Se
     * mandan las tres señales por separado -y no un unico "esta en riesgo"-
     * para que la pantalla pueda explicar el motivo: quien mira el panel
     * tiene que saber si llamar al representante por plata, por faltas o
     * por una lesion.
     */
    public record EstudianteEnRiesgoResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            boolean mensualidadPendiente,
            boolean asistenciaBaja,
            /** Null si su categoria no tuvo sesiones programadas en el rango. */
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
            List<EstudianteEnRiesgoResponse> estudiantes
    ) {}
}
