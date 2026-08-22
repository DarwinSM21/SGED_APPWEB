package org.uteq.backend.common.ia;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
    private final int reintentos;

    public GeminiFeedbackService(
            @Value("${ia.gemini.api-key:}") String apiKey,
            @Value("${ia.gemini.modelo:gemini-2.0-flash}") String modelo,
            @Value("${ia.gemini.habilitado:false}") boolean habilitado,
            @Value("${ia.gemini.timeout-segundos:8}") int timeoutSegundos,
            @Value("${ia.gemini.reintentos:2}") int reintentos) {

        this.apiKey = apiKey;
        this.modelo = modelo;
        this.habilitado = habilitado && apiKey != null && !apiKey.isBlank();
        this.reintentos = Math.max(0, reintentos);

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

    /**
     * Reintenta ante fallos transitorios del proveedor. El nivel gratuito de
     * Gemini devuelve 503 UNAVAILABLE de forma intermitente: midiendo 5
     * llamadas seguidas con el mismo prompt respondieron 2 (los fallos
     * fueron 503, no errores de la peticion). Con dos reintentos la
     * probabilidad de quedarse sin comentario baja de ~60% a ~22% sin tocar
     * el resto del sistema.
     *
     * <p>No se reintenta ante un 4xx -clave invalida, cuota agotada, cuerpo
     * mal formado-: ahi el problema es nuestro y repetir solo suma demora.
     */
    private ResultadoFeedback invocar(String prompt) {
        Exception ultimoFallo = null;

        for (int intento = 0; intento <= reintentos; intento++) {
            try {
                return intentarUnaVez(prompt);

            } catch (HttpClientErrorException e) {
                // 4xx: no tiene sentido repetir.
                log.warn("Gemini rechazo la peticion: {}", e.getStatusCode());
                return ResultadoFeedback.noDisponible(motivoDeRechazo(e));

            } catch (Exception e) {
                ultimoFallo = e;
                // Se registra el tipo de fallo, nunca el prompt: aunque va
                // seudonimizado, no hay razon para duplicarlo en los logs.
                log.warn("Fallo transitorio de Gemini (intento {} de {}): {}",
                        intento + 1, reintentos + 1, e.getClass().getSimpleName());
                if (intento < reintentos) {
                    esperarAntesDeReintentar(intento);
                }
            }
        }

        log.warn("No se pudo generar feedback con IA tras {} intento(s): {}",
                reintentos + 1, ultimoFallo == null ? "sin detalle" : ultimoFallo.getClass().getSimpleName());
        return ResultadoFeedback.noDisponible("El servicio de generacion no respondio");
    }

    /**
     * Traduce el 4xx a algo que el entrenador pueda entender y actuar.
     *
     * <p>Decir "no respondio" ante un 429 es directamente falso -si respondio,
     * y respondio que se acabo la cuota del dia- y manda a buscar el problema
     * en la red, que es donde no esta. El nivel gratuito de Gemini tiene un
     * limite diario: una tarde de pruebas lo agota, y al dia siguiente vuelve
     * solo.
     */
    private String motivoDeRechazo(HttpClientErrorException e) {
        int codigo = e.getStatusCode().value();
        if (codigo == 429) {
            return "Se agoto la cuota diaria del servicio de IA; se restablece manana";
        }
        if (codigo == 401 || codigo == 403) {
            return "La clave del servicio de IA no es valida o no tiene permisos";
        }
        return "El servicio de IA rechazo la peticion (codigo " + codigo + ")";
    }

    private ResultadoFeedback intentarUnaVez(String prompt) {
        var cuerpo = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", INSTRUCCION_SISTEMA))),
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        // Los modelos "-latest" actuales razonan antes de responder y ese
                        // pensamiento interno tambien consume maxOutputTokens (~600-700
                        // tokens tipicos vistos en pruebas reales); con un limite bajo la
                        // respuesta visible quedaba cortada a mitad de frase.
                        "maxOutputTokens", 2048));

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
            // respuesta: hay 200 OK pero sin contenido utilizable. No es
            // transitorio, asi que no se reintenta.
            log.warn("Gemini respondio sin texto utilizable (posible bloqueo por filtro de seguridad)");
            return ResultadoFeedback.noDisponible("El modelo no devolvio texto");
        }
        return ResultadoFeedback.ok(texto.trim());
    }

    /** Espera creciente y corta (0,4s y 0,8s): el entrenador esta mirando la pantalla. */
    private void esperarAntesDeReintentar(int intento) {
        try {
            Thread.sleep(400L * (intento + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
