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

    /**
     * El informe puesto en palabras, para quien no lee numeros.
     *
     * <p>Un padre ve "Tactica 5.5" y no sabe si eso es bueno. El comentario
     * traduce; no agrega informacion que no este en los mismos numeros que ya
     * tiene arriba.
     *
     * @param disponible false si el modelo no respondio. No es un error: el
     *                   informe se muestra igual, con sus numeros.
     */
    public record ComentarioInformeResponse(
            String comentario,
            boolean disponible,
            String motivo
    ) {}
}
