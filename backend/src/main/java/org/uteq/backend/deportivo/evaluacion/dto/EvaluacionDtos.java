package org.uteq.backend.deportivo.evaluacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO del modulo de evaluacion diaria.
 *
 * <p>Van juntos a proposito: son las piezas de una sola pantalla y leerlos
 * como conjunto explica el flujo mejor que ocho archivos sueltos.
 */
public final class EvaluacionDtos {

    private EvaluacionDtos() {}

    /**
     * Un jugador tal como lo ve el entrenador al abrir la sesion.
     *
     * @param puntajes       lo ya guardado hoy, o la precarga del entrenamiento
     *                       anterior si todavia no se califico
     * @param precargado     true si los puntajes vienen del dia previo y aun no
     *                       se han confirmado hoy; la interfaz puede marcarlos
     *                       para que el entrenador sepa que son heredados
     * @param lesionado      arrastra lesion activa
     * @param puedeEvaluarse false si no marco asistencia: la ficha va bloqueada
     */
    public record JugadorEvaluableResponse(
            Long idEstudiante,
            String nombreCompleto,
            String categoria,
            Long idPosicion,
            String posicion,
            Map<String, BigDecimal> puntajes,
            boolean precargado,
            boolean lesionado,
            boolean puedeEvaluarse,
            String motivoBloqueo
    ) {}

    /** Pantalla completa de evaluacion de una sesion. */
    public record EvaluacionSesionResponse(
            Long idEvaluacion,
            Long idSesion,
            LocalDate fecha,
            String categoria,
            String estado,
            List<CriterioResponse> criterios,
            List<JugadorEvaluableResponse> jugadores,
            String observacionGeneral
    ) {}

    public record CriterioResponse(
            Long idCriterio,
            String nombre,
            String descripcion,
            Short puntajeMaximo
    ) {}

    /**
     * Guardado incremental de un jugador. El entrenador mueve un slider y la
     * interfaz manda esto sin esperar a que termine: el documento del modulo
     * pide autoguardado para no perder trabajo.
     */
    public record GuardarJugadorRequest(
            @NotNull Long idEstudiante,
            Long idPosicionJugada,
            @NotNull List<PuntajeCriterioRequest> puntajes
    ) {}

    public record PuntajeCriterioRequest(
            @NotNull Long idCriterio,
            @NotNull
            @DecimalMin(value = "0.0", message = "El puntaje no puede ser negativo")
            @DecimalMax(value = "10.0", message = "El puntaje no puede superar 10")
            BigDecimal puntaje
    ) {}

    /** Comentario generado para un jugador. */
    public record FeedbackResponse(
            Long idEstudiante,
            String texto,
            boolean generadoPorIa,
            String motivoNoDisponible
    ) {}

    /** Alineacion sugerida. */
    public record PlantillaResponse(
            Long idSesion,
            String categoria,
            List<JugadorPlantillaResponse> titulares,
            List<JugadorPlantillaResponse> suplentes,
            List<Long> excluidosPorLesion,
            String comentario,
            boolean comentarioGeneradoPorIa
    ) {}

    public record JugadorPlantillaResponse(
            Long idEstudiante,
            String nombreCompleto,
            String posicion,
            BigDecimal promedioAcumulado
    ) {}
}
