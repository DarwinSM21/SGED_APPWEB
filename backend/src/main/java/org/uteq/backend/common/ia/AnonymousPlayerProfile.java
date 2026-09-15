package org.uteq.backend.common.ia;

import java.util.Map;

/**
 * Perfil de un jugador sin datos identificativos, para enviar a un proveedor
 * externo de generación de texto (RNF-16, frontera de datos al LLM).
 *
 * @param reference             referencia anónima que reemplaza el nombre real
 * @param category              categoría deportiva del jugador
 * @param position              posición de juego
 * @param scores                puntajes actuales por criterio de evaluación
 * @param previousScores        puntajes del periodo anterior, para comparar
 * @param lastMonthAttendances  asistencias del último mes
 * @param injured               {@code true} si tiene una lesión activa
 */
public record AnonymousPlayerProfile(
        String reference, String category, String position,
        Map<String, Double> scores, Map<String, Double> previousScores,
        Integer lastMonthAttendances, boolean injured
) {
    /**
     * Valida que la referencia anónima esté presente y normaliza los mapas
     * de puntajes a copias inmutables no nulas ({@link Map#of()} si vienen
     * nulos), para que el resto del código no tenga que comprobar null.
     *
     * @throws IllegalArgumentException si {@code reference} es nulo o está en blanco
     */
    public AnonymousPlayerProfile {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("La referencia anonima es obligatoria");
        }
        scores = scores == null ? Map.of() : Map.copyOf(scores);
        previousScores = previousScores == null ? Map.of() : Map.copyOf(previousScores);
    }
}
