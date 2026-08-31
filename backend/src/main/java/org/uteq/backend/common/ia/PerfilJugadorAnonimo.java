package org.uteq.backend.common.ia;

import java.util.Map;

public record PerfilJugadorAnonimo(
        String referencia,
        String categoria,
        String posicion,
        Map<String, Double> puntajes,
        Map<String, Double> puntajesPrevios,
        Integer asistenciasUltimoMes,
        boolean lesionado
) {
    public PerfilJugadorAnonimo {
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("La referencia anonima es obligatoria");
        }
        puntajes = puntajes == null ? Map.of() : Map.copyOf(puntajes);
        puntajesPrevios = puntajesPrevios == null ? Map.of() : Map.copyOf(puntajesPrevios);
    }
}
