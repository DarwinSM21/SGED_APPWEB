package org.uteq.backend.common.ia;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementacion de {@link GeneradorFeedbackIA} contra la API de Gemini.
 *
 * <p>Decision de proveedor y de costo tomada por el equipo: nivel gratuito de
 * Gemini, suficiente para el volumen del proyecto (~30 estudiantes/dia frente
 * a limites de 250-1000 solicitudes diarias).
 *
 * <p>La clave se pasa en la cabecera {@code x-goog-api-key} y no como
 * parametro de consulta. Google documenta ambas formas, pero un parametro de
 * consulta viaja en la URL y termina en logs de acceso, historiales de proxy y
 * cabeceras {@code Referer}. Para un proyecto con auditoria OWASP encima, la
 * cabecera es la unica opcion defendible.
 */
@Service
public class GeminiFeedbackService implements GeneradorFeedbackIA {

    private static final Logger log = LoggerFactory.getLogger(GeminiFeedbackService.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    /**
     * Instruccion de sistema. Acota el registro y, sobre todo, prohibe al
     * modelo inventar datos: el entrenador tiene que poder confiar en que el
     * texto describe lo que realmente se midio.
     */
    private static final String INSTRUCCION_SISTEMA = """
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

    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;
    private final boolean habilitado;

    public GeminiFeedbackService(
            @Value("${ia.gemini.api-key:}") String apiKey,
            @Value("${ia.gemini.modelo:gemini-2.0-flash}") String modelo,
            @Value("${ia.gemini.habilitado:false}") boolean habilitado,
            @Value("${ia.gemini.timeout-segundos:8}") int timeoutSegundos) {

        this.apiKey = apiKey;
        this.modelo = modelo;
        this.habilitado = habilitado && apiKey != null && !apiKey.isBlank();

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        // Timeouts cortos a proposito: el entrenador califica desde el celular
        // en la cancha, con mala conexion. Es preferible quedarse sin
        // comentario a dejar la interfaz colgada.
        factory.setConnectTimeout((int) Duration.ofSeconds(timeoutSegundos).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(timeoutSegundos).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();

        if (!this.habilitado) {
            log.info("Feedback con IA deshabilitado: no hay clave de Gemini configurada "
                    + "o ia.gemini.habilitado=false. El sistema funciona sin comentarios generados.");
        }
    }

    @Override
    public boolean estaDisponible() {
        return habilitado;
    }

    @Override
    public ResultadoFeedback generarComentarioJugador(PerfilJugadorAnonimo perfil) {
        if (!habilitado) {
            return ResultadoFeedback.noDisponible("Generacion de texto deshabilitada");
        }
        return invocar(construirPromptJugador(perfil));
    }

    @Override
    public ResultadoFeedback generarComentarioPlantilla(List<PerfilJugadorAnonimo> alineacion) {
        if (!habilitado) {
            return ResultadoFeedback.noDisponible("Generacion de texto deshabilitada");
        }
        if (alineacion == null || alineacion.isEmpty()) {
            return ResultadoFeedback.noDisponible("La alineacion esta vacia");
        }
        return invocar(construirPromptPlantilla(alineacion));
    }

    // ------------------------------------------------------------------
    // Construccion de prompts
    //
    // Solo se serializan campos de PerfilJugadorAnonimo. Ese record no tiene
    // nombre, cedula, correo ni fecha de nacimiento, de modo que aqui no hay
    // forma de filtrar identidad aunque se quisiera.
    // ------------------------------------------------------------------

    private String construirPromptJugador(PerfilJugadorAnonimo p) {
        var sb = new StringBuilder();
        sb.append("Redacta la retroalimentacion del entrenamiento de hoy para este jugador.\n\n");
        sb.append("Categoria: ").append(p.categoria()).append('\n');
        if (p.posicion() != null) {
            sb.append("Posicion en la que jugo: ").append(p.posicion()).append('\n');
        }
        sb.append("Puntajes de hoy (sobre 10): ").append(formatear(p.puntajes())).append('\n');
        if (!p.puntajesPrevios().isEmpty()) {
            sb.append("Promedio historico: ").append(formatear(p.puntajesPrevios())).append('\n');
        }
        if (p.asistenciasUltimoMes() != null) {
            sb.append("Sesiones asistidas en el ultimo mes: ").append(p.asistenciasUltimoMes()).append('\n');
        }
        if (p.lesionado()) {
            sb.append("Arrastra una lesion activa: no sugieras aumentar la carga fisica.\n");
        }
        return sb.toString();
    }

    private String construirPromptPlantilla(List<PerfilJugadorAnonimo> alineacion) {
        var sb = new StringBuilder();
        sb.append("Comenta brevemente esta alineacion, ya seleccionada por el sistema ")
          .append("segun puntaje acumulado. No propongas cambios de jugadores: ")
          .append("explica que fortalezas tiene el once planteado.\n\n");
        for (var p : alineacion) {
            sb.append("- ").append(p.referencia())
              .append(" (").append(p.posicion() == null ? "sin posicion" : p.posicion()).append("): ")
              .append(formatear(p.puntajes())).append('\n');
        }
        return sb.toString();
    }

    private String formatear(Map<String, Double> puntajes) {
        if (puntajes.isEmpty()) {
            return "sin datos";
        }
        return puntajes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    // ------------------------------------------------------------------
    // Invocacion
    // ------------------------------------------------------------------

    private ResultadoFeedback invocar(String prompt) {
        try {
            var cuerpo = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", INSTRUCCION_SISTEMA))),
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "maxOutputTokens", 200));

            JsonNode respuesta = restClient.post()
                    .uri("/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);

            String texto = extraerTexto(respuesta);
            if (texto == null || texto.isBlank()) {
                // Ocurre cuando el filtro de seguridad del proveedor bloquea la
                // respuesta: hay 200 OK pero sin contenido utilizable.
                log.warn("Gemini respondio sin texto utilizable (posible bloqueo por filtro de seguridad)");
                return ResultadoFeedback.noDisponible("El modelo no devolvio texto");
            }
            return ResultadoFeedback.ok(texto.trim());

        } catch (Exception e) {
            // Se registra el tipo de fallo, nunca el prompt: aunque va
            // seudonimizado, no hay razon para duplicarlo en los logs.
            log.warn("No se pudo generar feedback con IA: {}", e.getClass().getSimpleName());
            return ResultadoFeedback.noDisponible("El servicio de generacion no respondio");
        }
    }

    private String extraerTexto(JsonNode respuesta) {
        if (respuesta == null) {
            return null;
        }
        JsonNode parts = respuesta.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }
        return parts.get(0).path("text").asText(null);
    }
}
