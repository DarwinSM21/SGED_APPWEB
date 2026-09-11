package org.uteq.backend.common.ia;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Genera comentarios de evaluación en lenguaje natural usando una API
 * compatible con OpenAI. Expuesto como {@code AIFeedbackGenerator} y activo
 * solo cuando la propiedad {@code ia.proveedor=openai}. Sin clave configurada
 * o ante fallos del servicio externo, devuelve un {@code FeedbackResult}
 * "no disponible" en lugar de romper la petición.
 */
@Service
@ConditionalOnProperty(name = "ia.proveedor", havingValue = "openai")
public class OpenAiFeedbackService implements AIFeedbackGenerator {
    private static final Logger log = LoggerFactory.getLogger(OpenAiFeedbackService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;
    private final boolean habilitado;
    private final int reintentos;

    /**
     * Configura el cliente HTTP contra la API compatible con OpenAI y registra
     * si el generador queda habilitado (requiere clave no vacía).
     *
     * @param apiKey          clave de la API (pe. {@code sk-...})
     * @param baseUrl         URL base del servicio de chat compatible
     * @param modelo          identificador del modelo a usar (p. ej.
     *                        {@code gpt-4o-mini})
     * @param timeoutSegundos tiempo máximo de conexión y lectura, en segundos
     * @param reintentos      reintentos ante fallos transitorios (mínimo 0)
     */
    public OpenAiFeedbackService(
            @Value("${ia.openai.api-key:}") String apiKey,
            @Value("${ia.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ia.openai.modelo:gpt-4o-mini}") String modelo,
            @Value("${ia.openai.timeout-segundos:10}") int timeoutSegundos,
            @Value("${ia.openai.reintentos:1}") int reintentos) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = modelo;
        this.reintentos = Math.max(0, reintentos);
        this.habilitado = !this.apiKey.isBlank();

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(timeoutSegundos).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(timeoutSegundos).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        if (!habilitado) {
            log.warn("Proveedor de IA 'openai' seleccionado pero sin clave: el feedback quedara no disponible");
        } else {
            log.info("Generacion de texto por API compatible con OpenAI: {} , modelo {}", baseUrl, modelo);
        }
    }

    /**
     * @return {@code true} si el proveedor compatible con OpenAI está habilitado y tiene clave configurada
     */
    @Override
    public boolean isAvailable() {
        return habilitado;
    }

    /**
     * Genera el comentario de evaluación de un jugador anónimo.
     *
     * @param profile datos anonimizados del jugador a evaluar
     * @return resultado con el texto generado o {@code unavailable} si el
     *         servicio está deshabilitado o falla
     */
    @Override
    public FeedbackResult generatePlayerComment(AnonymousPlayerProfile profile) {
        if (!habilitado) {
            return FeedbackResult.unavailable("Generacion de texto deshabilitada");
        }
        return invocar(PromptsFeedback.deJugador(profile));
    }

    /**
     * Genera el comentario de evaluación de una alineación completa.
     *
     * @param lineup lista de perfiles anónimos que forman la plantilla
     * @return resultado con el texto generado o {@code unavailable} si el
     *         servicio está deshabilitado, la alineación está vacía o falla
     */
    @Override
    public FeedbackResult generateLineupComment(List<AnonymousPlayerProfile> lineup) {
        if (!habilitado) {
            return FeedbackResult.unavailable("Generacion de texto deshabilitada");
        }
        if (lineup == null || lineup.isEmpty()) {
            return FeedbackResult.unavailable("La alineacion esta vacia");
        }
        return invocar(PromptsFeedback.dePlantilla(lineup));
    }

    private FeedbackResult invocar(String prompt) {
        Exception ultimoFallo = null;

        for (int intento = 0; intento <= reintentos; intento++) {
            try {
                return intentarUnaVez(prompt);

            } catch (HttpClientErrorException e) {
                log.warn("OpenAI rechazo la peticion: {}", e.getStatusCode());
                return FeedbackResult.unavailable(motivoDeRechazo(e));

            } catch (Exception e) {
                ultimoFallo = e;

                log.warn("Fallo transitorio de OpenAI (intento {} de {}): {}",
                        intento + 1, reintentos + 1, e.getClass().getSimpleName());
                if (intento < reintentos) {
                    esperarAntesDeReintentar(intento);
                }
            }
        }

        log.warn("No se pudo generar feedback con IA tras {} intento(s): {}",
                reintentos + 1, ultimoFallo == null ? "sin detalle" : ultimoFallo.getClass().getSimpleName());
        return FeedbackResult.unavailable("El servicio de generacion no respondio");
    }

    private FeedbackResult intentarUnaVez(String prompt) {
        var cuerpo = Map.of(
                "model", modelo,
                "messages", List.of(
                        Map.of("role", "system", "content", PromptsFeedback.INSTRUCCION_SISTEMA),
                        Map.of("role", "user", "content", prompt)),
                "temperature", 0.4,
                "max_tokens", 800);

        JsonNode respuesta = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(JsonNode.class);

        String texto = extraerTexto(respuesta);
        if (texto == null || texto.isBlank()) {
            String motivoCorte = respuesta.path("choices").path(0).path("finish_reason").asText("");
            if ("length".equals(motivoCorte)) {
                log.warn("El modelo agoto el presupuesto de tokens antes de escribir "
                        + "(finish_reason=length); revisar max_tokens");
                return FeedbackResult.unavailable(
                        "El modelo se quedo sin espacio para responder");
            }
            log.warn("OpenAI respondio sin texto utilizable (finish_reason={})", motivoCorte);
            return FeedbackResult.unavailable("El modelo no devolvio texto");
        }
        return FeedbackResult.ok(texto.trim());
    }

    private String motivoDeRechazo(HttpClientErrorException e) {
        int codigo = e.getStatusCode().value();
        if (codigo == 429) {
            return "Se agoto la cuota del servicio de IA o se superó el limite de peticiones";
        }
        if (codigo == 401 || codigo == 403) {
            return "La clave del servicio de IA no es valida o fue revocada";
        }
        return "El servicio de IA rechazo la peticion (codigo " + codigo + ")";
    }

    private String extraerTexto(JsonNode respuesta) {
        if (respuesta == null) {
            return null;
        }
        JsonNode contenido = respuesta.path("choices").path(0).path("message").path("content");
        return contenido.isTextual() ? contenido.asText() : null;
    }

    private void esperarAntesDeReintentar(int intento) {
        try {
            Thread.sleep(400L * (intento + 1));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
