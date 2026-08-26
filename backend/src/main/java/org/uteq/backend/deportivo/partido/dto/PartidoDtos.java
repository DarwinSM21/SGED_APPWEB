package org.uteq.backend.deportivo.partido.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class PartidoDtos {

    public record CrearPartidoRequest(
            @NotNull(message = "Elegí la categoría que juega")
            Long idCategoria,

            @NotNull(message = "Indicá la fecha del partido")
            LocalDate fecha,

            LocalTime hora,

            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String observacion
    ) {}

    /**
     * El marcador va aparte de la creacion porque llega despues: cuando se
     * agenda el partido todavia no se jugo. Los dos goles son obligatorios
     * juntos -"metimos 3" sin saber cuantos recibimos no dice si se gano-.
     */
    public record ResultadoRequest(
            @NotNull(message = "Faltan los goles a favor")
            @Min(value = 0, message = "Los goles no pueden ser negativos")
            @Max(value = 99, message = "Revisá el marcador")
            Short golesFavor,

            @NotNull(message = "Faltan los goles en contra")
            @Min(value = 0, message = "Los goles no pueden ser negativos")
            @Max(value = 99, message = "Revisá el marcador")
            Short golesContra,

            @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
            String observacion
    ) {}

    public record PartidoResponse(
            Long idPartido,
            Long idCategoria,
            String categoria,
            LocalDate fecha,
            LocalTime hora,
            Short golesFavor,
            Short golesContra,
            String observacion,
            /** GANADO | EMPATADO | PERDIDO | PENDIENTE. Calculado, no almacenado. */
            String resultado,
            /** true si ya hay una alineación guardada para este partido. */
            boolean tieneAlineacion,
            /** Cuántos titulares tiene guardados; 0 si todavía no se armó. */
            int titulares
    ) {}

    public record PartidoPageResponse(
            List<PartidoResponse> contenido,
            int pagina,
            int tamano,
            long total,
            int totalPaginas
    ) {}
}
