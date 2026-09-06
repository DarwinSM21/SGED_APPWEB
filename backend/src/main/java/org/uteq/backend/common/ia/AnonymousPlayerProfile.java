package org.uteq.backend.common.ia;

import java.util.Map;

public record AnonymousPlayerProfile(
        String reference, String category, String position,
        Map<String, Double> scores, Map<String, Double> previousScores,
        Integer lastMonthAttendances, boolean injured
) {
    public AnonymousPlayerProfile {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("La referencia anonima es obligatoria");
        }
        scores = scores == null ? Map.of() : Map.copyOf(scores);
        previousScores = previousScores == null ? Map.of() : Map.copyOf(previousScores);
    }
}
