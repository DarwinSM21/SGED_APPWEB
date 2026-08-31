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
            BigDecimal porcentajeAsistencia
    ) {}

    public record ComentarioInformeResponse(
            String comentario,
            boolean disponible,
            String motivo
    ) {}
}
