package org.uteq.backend.deportivo.partido.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ConvocatoriaDtos {
    public record JugadorConvocado(
            Long idEstudiante,
            String nombreCompleto,
            String posicion,
            Long idPosicion,
            boolean titular,
            BigDecimal promedio,
            long presencias,
            long entrenamientos
    ) {}

    public record NoConvocable(
            Long idEstudiante,
            String nombreCompleto,
            String motivo
    ) {}

    public record VentanaRendimiento(
            int semanas,
            LocalDate desde,
            LocalDate hasta,
            long entrenamientos
    ) {}

    public record AlineacionResponse(
            Long idPartido,
            Long idCategoria,
            String categoria,
            LocalDate fecha,
            boolean guardada,
            Short valoracion,
            String observacion,
            VentanaRendimiento ventana,
            List<JugadorConvocado> titulares,
            List<JugadorConvocado> suplentes,
            List<JugadorConvocado> disponibles,
            List<NoConvocable> noConvocables,
            int cupoTitulares
    ) {}

    public record FeedbackAlineacionResponse(
            String comentario,
            boolean disponible,
            String motivo
    ) {}
}
