package org.uteq.backend.common.ia;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Texto que se le manda al modelo, independiente del proveedor.
 *
 * <p>Vive aparte porque lo que se pide y lo que se prohibe pedir es una
 * decision del proyecto, no de Google ni de OpenAI. Al cambiar de proveedor
 * cambia la forma de la peticion HTTP, nunca el contenido: si los prompts
 * estuvieran copiados en cada implementacion, un ajuste al tono acabaria
 * aplicado en una y olvidado en la otra.
 *
 * <p>Solo se serializan campos de {@link PerfilJugadorAnonimo}. Ese record no
 * tiene nombre, cedula, correo ni fecha de nacimiento, de modo que aqui no hay
 * forma de filtrar identidad aunque se quisiera.
 */
final class PromptsFeedback {

    private PromptsFeedback() {
    }

    /**
     * Instruccion de sistema. Acota el registro y, sobre todo, prohibe al
     * modelo inventar datos: el entrenador tiene que poder confiar en que el
     * texto describe lo que realmente se midio.
     */
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

    /**
     * Resumen del rendimiento acumulado de un jugador, escrito para su
     * familia.
     *
     * <p>El destinatario cambia el texto. Un representante ve "Tactica 5.5"
     * y no sabe si eso es bueno, malo o normal para la edad, asi que se le
     * pide al modelo que TRADUZCA los numeros en vez de repetirlos, y que no
     * los presente como un veredicto sobre el chico.
     */
    static String deJugador(PerfilJugadorAnonimo p) {
        var sb = new StringBuilder();
        sb.append("Resume como viene rindiendo este jugador, para que lo lea su padre, ")
          .append("madre o representante. Traduce los numeros a lenguaje corriente en vez ")
          .append("de repetirlos: quien lo lee no sabe si un 5.5 es bueno o malo. Di en que ")
          .append("viene mejor y en que le cuesta mas, sin emitir un veredicto sobre el ")
          .append("chico.\n\n");
        sb.append("Categoria: ").append(p.categoria()).append('\n');
        if (p.posicion() != null) {
            sb.append("Posicion en la que juega: ").append(p.posicion()).append('\n');
        }
        sb.append("Promedio por criterio (sobre 10): ").append(formatear(p.puntajes())).append('\n');
        if (!p.puntajesPrevios().isEmpty()) {
            sb.append("Promedio historico: ").append(formatear(p.puntajesPrevios())).append('\n');
        }
        if (p.asistenciasUltimoMes() != null) {
            sb.append("Entrenamientos a los que asistio en el ultimo mes: ")
              .append(p.asistenciasUltimoMes()).append('\n');
        }
        if (p.lesionado()) {
            sb.append("Arrastra una lesion activa: no sugieras aumentar la carga fisica.\n");
        }
        return sb.toString();
    }

    static String dePlantilla(List<PerfilJugadorAnonimo> alineacion) {
        var sb = new StringBuilder();
        sb.append("Comenta brevemente esta alineacion, ya seleccionada por el sistema ")
          .append("segun puntaje acumulado. No propongas cambios de jugadores ni de posiciones: ")
          .append("eso ya lo decidio el algoritmo. Basandote solo en los puntajes dados, ")
          .append("señala una fortaleza del once planteado y un aspecto a vigilar ")
          .append("(por ejemplo un puntaje mas bajo en alguna posicion o criterio).\n\n");
        for (var p : alineacion) {
            sb.append("- ").append(p.referencia())
              .append(" (").append(p.posicion() == null ? "sin posicion" : p.posicion()).append("): ")
              .append(formatear(p.puntajes())).append('\n');
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
