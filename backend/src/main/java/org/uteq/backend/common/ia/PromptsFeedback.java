package org.uteq.backend.common.ia;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class PromptsFeedback {
    private PromptsFeedback() {
    }

    static final String INSTRUCCION_SISTEMA = """
            Eres un asistente que redacta retroalimentacion deportiva para una escuela
            de futbol formativo con jugadores en edad escolar.
            Reglas que debes cumplir siempre:
            - Escribe en espanol neutro, en segunda persona del plural o impersonal.
            - Maximo 3 frases. Sin listas, sin titulos, sin emojis.
            - Basate unicamente en los datos numericos que recibes. No inventes
              hechos, incidentes ni cualidades que no esten en los datos.
            - Tono constructivo y apropiado para un menor de edad: senala un punto
              fuerte y un aspecto a mejorar, nunca descalifiques a la persona.
            - No hagas diagnosticos medicos ni recomendaciones de salud.
            - Si un jugador arrastra una lesion, no sugieras aumentar su carga fisica.
            """;

    static String deJugador(AnonymousPlayerProfile p) {
        var sb = new StringBuilder();
        sb.append("Resume como viene rindiendo este jugador, para que lo lea su padre, ")
          .append("madre o representante. Traduce los numeros a lenguaje corriente en vez ")
          .append("de repetirlos: quien lo lee no sabe si un 5.5 es bueno o malo. Di en que ")
          .append("viene mejor y en que le cuesta mas, sin emitir un veredicto sobre el ")
          .append("chico.\n\n");
        sb.append("Categoria: ").append(p.category()).append('\n');
        if (p.position() != null) {
            sb.append("Posicion en la que juega: ").append(p.position()).append('\n');
        }
        sb.append("Promedio por criterio (sobre 10): ").append(formatear(p.scores())).append('\n');
        if (!p.previousScores().isEmpty()) {
            sb.append("Promedio historico: ").append(formatear(p.previousScores())).append('\n');
        }
        if (p.lastMonthAttendances() != null) {
            sb.append("Entrenamientos a los que asistio en el ultimo mes: ")
              .append(p.lastMonthAttendances()).append('\n');
        }
        if (p.injured()) {
            sb.append("Arrastra una lesion activa: no sugieras aumentar la carga fisica.\n");
        }
        return sb.toString();
    }

    static String dePlantilla(List<AnonymousPlayerProfile> alineacion) {
        var sb = new StringBuilder();
        sb.append("Comenta brevemente esta alineacion, ya seleccionada por el sistema ")
          .append("segun puntaje acumulado. No propongas cambios de jugadores ni de posiciones: ")
          .append("eso ya lo decidio el algoritmo. Basandote solo en los puntajes dados, ")
          .append("señala una fortaleza del once planteado y un aspecto a vigilar ")
          .append("(por ejemplo un puntaje mas bajo en alguna posicion o criterio).\n\n");
        for (var p : alineacion) {
            sb.append("- ").append(p.reference())
              .append(" (").append(p.position() == null ? "sin posicion" : p.position()).append("): ")
              .append(formatear(p.scores())).append('\n');
        }
        return sb.toString();
    }

    private static String formatear(Map<String, Double> puntajes) {
        if (puntajes.isEmpty()) {
            return "sin datos";
        }
        return puntajes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
