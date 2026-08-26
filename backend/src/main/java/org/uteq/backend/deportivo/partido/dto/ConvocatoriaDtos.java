package org.uteq.backend.deportivo.partido.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que se muestra al armar el once de un partido.
 *
 * <p>Cada jugador viaja con los numeros que lo pusieron donde esta -promedio
 * de las ultimas semanas y cuantos entrenamientos piso-, no solo con su
 * nombre. Es lo que permite que el entrenador vea POR QUE el sistema lo
 * sugirio ahi, y lo que le da con que responder cuando un padre pregunta por
 * que su hijo quedo en el banco.
 */
public class ConvocatoriaDtos {

    public record JugadorConvocado(
            Long idEstudiante,
            String nombreCompleto,
            /** Abreviatura del puesto que ocupa (POR, DFC…). */
            String posicion,
            Long idPosicion,
            boolean titular,
            /** Promedio en la ventana. null = no lo evaluaron ni una vez ahí. */
            BigDecimal promedio,
            long presencias,
            long entrenamientos
    ) {}

    public record NoConvocable(
            Long idEstudiante,
            String nombreCompleto,
            String motivo
    ) {}

    /** La ventana de rendimiento con la que se calculó la sugerencia. */
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
            /** true = la guardó el entrenador; false = es la sugerencia del sistema. */
            boolean guardada,
            Short valoracion,
            String observacion,
            VentanaRendimiento ventana,
            List<JugadorConvocado> titulares,
            List<JugadorConvocado> suplentes,
            /** Convocables que no están en el once: de aquí salen los cambios. */
            List<JugadorConvocado> disponibles,
            /** Quién no puede jugar y por qué. Se muestra, no se esconde. */
            List<NoConvocable> noConvocables,
            /** Tope de titulares configurado, para que la pantalla no lo adivine. */
            int cupoTitulares
    ) {}

    public record FeedbackAlineacionResponse(
            String comentario,
            boolean disponible,
            String motivo
    ) {}
}
