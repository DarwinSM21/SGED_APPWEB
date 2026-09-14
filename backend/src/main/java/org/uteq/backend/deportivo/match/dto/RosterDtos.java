package org.uteq.backend.deportivo.match.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Contenedor de los DTO de convocatoria y sugerencia de alineación. */
public class RosterDtos {
    /**
     * Un jugador convocado o convocable a un partido.
     *
     * @param studentId          identificador del estudiante
     * @param fullName           nombre completo
     * @param position           nombre de la posición
     * @param positionId         identificador de la posición
     * @param starter            {@code true} si va de titular en la alineación guardada
     * @param average            promedio de evaluación en la ventana de rendimiento
     * @param attendanceRecords  cantidad de registros de asistencia en la ventana
     * @param trainingSessions   cantidad de sesiones de entrenamiento en la ventana
     */
    public record CalledUpPlayer(
            Long studentId,
            String fullName,
            String position,
            Long positionId,
            boolean starter,
            BigDecimal average,
            long attendanceRecords,
            long trainingSessions
    ) {}

    /**
     * Un estudiante que no puede ser convocado.
     *
     * @param studentId  identificador del estudiante
     * @param fullName   nombre completo
     * @param reason     motivo por el que no puede convocarse (p. ej. lesión activa)
     */
    public record UnavailablePlayer(
            Long studentId,
            String fullName,
            String reason
    ) {}

    /**
     * Ventana de tiempo usada para calcular el rendimiento reciente de los jugadores.
     *
     * @param weeks             cantidad de semanas consideradas
     * @param from              fecha inicial de la ventana
     * @param to                fecha final de la ventana
     * @param trainingSessions  sesiones de entrenamiento dentro de la ventana
     */
    public record PerformanceWindow(
            int weeks,
            LocalDate from,
            LocalDate to,
            long trainingSessions
    ) {}

    /**
     * Alineación sugerida o guardada de un partido, con los jugadores disponibles.
     *
     * @param matchId       identificador del partido
     * @param categoryId    identificador de la categoría
     * @param category      nombre de la categoría
     * @param date          fecha del partido
     * @param saved         {@code true} si la alineación ya fue guardada por el entrenador
     * @param rating        valoración del entrenador, si la guardó
     * @param note          observación del entrenador, si la guardó
     * @param window        ventana de rendimiento usada para el ranking de jugadores
     * @param starters      titulares sugeridos o guardados
     * @param substitutes   suplentes sugeridos o guardados
     * @param available     jugadores disponibles no incluidos en la alineación
     * @param notCallable   jugadores no convocables y su motivo
     * @param starterSlots  cantidad de titulares esperados según la formación
     * @param closed        {@code true} si el partido ya está cerrado
     */
    public record LineupResponse(
            Long matchId,
            Long categoryId,
            String category,
            LocalDate date,
            boolean saved,
            Short rating,
            String note,
            PerformanceWindow window,
            List<CalledUpPlayer> starters,
            List<CalledUpPlayer> substitutes,
            List<CalledUpPlayer> available,
            List<UnavailablePlayer> notCallable,
            int starterSlots,
            boolean closed
    ) {}

    /**
     * Retroalimentación textual sobre la alineación sugerida.
     *
     * @param comment    texto de retroalimentación, o {@code null} si no está disponible
     * @param available  {@code true} si hay retroalimentación disponible
     * @param reason     motivo por el que no está disponible, si aplica
     */
    public record LineupFeedbackResponse(
            String comment,
            boolean available,
            String reason
    ) {}
}
