package org.uteq.backend.academico.representante.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class InformeDtos {

    private InformeDtos() {}

    public record EstudianteResumenResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria
    ) {}

    public record PromedioCriterioResponse(String criterio, Double promedio) {}

    public record LesionResumenResponse(
            Long idLesion,
            String descripcion,
            LocalDate fechaLesion,
            LocalDate fechaEstimadaRetorno,
            LocalDate fechaAlta,
            boolean activa
    ) {}

    public record InformeEstudianteResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            List<PromedioCriterioResponse> promediosPorCriterio,
            List<LesionResumenResponse> historialLesiones,
            /** Asistencia de los ultimos 30 dias; null si su categoria no tuvo sesiones programadas en ese rango. */
            BigDecimal porcentajeAsistencia
    ) {}
}
