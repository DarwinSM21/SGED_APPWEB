package org.uteq.backend.common.ia;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Genera comentarios de evaluación en lenguaje natural usando la API de Google
 * Gemini. Expuesto como {@code GeneradorFeedbackIA} y activo solo cuando la
 * propiedad {@code ia.proveedor=gemini}. Si no hay clave configurada
 * ({@code ia.gemini.habilitado=false}) o el servicio externo falla, devuelve
 * un {@code ResultadoFeedback} "no disponible" en lugar de romper la petición.
 */
@Service
@ConditionalOnProperty(name = "ia.proveedor", havingValue = "gemini", matchIfMissing = true)
public class GeminiFeedbackService implements GeneradorFeedbackIA {
    private static final Logger log = LoggerFactory.getLogger(GeminiFeedbackService.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;
    private final boolean habilitado;
    private final int reintentos;

    /**
     * Configura el cliente HTTP contra Gemini y registra si el generador queda
     * habilitado (requiere clave no vacía y {@code ia.gemini.habilitado=true}).
     *
     * @param apiKey          clave de la API de Gemini
     * @param modelo          identificador del modelo a usar (p. ej.
     *                        {@code gemini-2.0-flash})
     * @param habilitado      si debe intentar generar texto
     * @param timeoutSegundos tiempo máximo de conexión y lectura, en segundos
     * @param reintentos      reintentos ante fallos transitorios (mínimo 0)
     */
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

    /**
     * Genera el comentario de evaluación de un jugador anónimo.
     *
     * @param perfil datos anonimizados del jugador a evaluar
     * @return resultado con el texto generado o {@code noDisponible} si el
     *         servicio está deshabilitado o falla
     */
    @Override
    public ResultadoFeedback generarComentarioJugador(PerfilJugadorAnonimo perfil) {
        if (!habilitado) {
            return ResultadoFeedback.noDisponible("Generacion de texto deshabilitada");
        }
        return invocar(PromptsFeedback.deJugador(perfil));
    }

    /**
     * Genera el comentario de evaluación de una alineación completa.
     *
     * @param alineacion lista de perfiles anónimos que forman la plantilla
     * @return resultado con el texto generado o {@code noDisponible} si el
     *         servicio está deshabilitado, la alineación está vacía o falla
     */
    @Override
    public ResultadoFeedback generarComentarioPlantilla(List<PerfilJugadorAnonimo> alineacion) {
        if (!habilitado) {
            return ResultadoFeedback.noDisponible("Generacion de texto deshabilitada");
        }
        if (alineacion == null || alineacion.isEmpty()) {
            return ResultadoFeedback.noDisponible("La alineacion esta vacia");
        }
        return invocar(PromptsFeedback.dePlantilla(alineacion));
    }

    private ResultadoFeedback invocar(String prompt) {
        Exception ultimoFallo = null;

        for (int intento = 0; intento <= reintentos; intento++) {
            try {
                return intentarUnaVez(prompt);

            } catch (HttpClientErrorException e) {
                log.warn("Gemini rechazo la peticion: {}", e.getStatusCode());
                return ResultadoFeedback.noDisponible(motivoDeRechazo(e));

            } catch (Exception e) {
                ultimoFallo = e;

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
                        "parts", List.of(Map.of("text", PromptsFeedback.INSTRUCCION_SISTEMA))),
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.4,
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
            log.warn("Gemini respondio sin texto utilizable (posible bloqueo por filtro de seguridad)");
            return ResultadoFeedback.noDisponible("El modelo no devolvio texto");
        }
        return ResultadoFeedback.ok(texto.trim());
    }

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
