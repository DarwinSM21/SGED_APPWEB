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
@ConditionalOnProperty(name = "ia.proveedor", havingValue = "gemini", matchIfMissing = true)
public class GeminiFeedbackService implements GeneradorFeedbackIA {

    private static final Logger log = LoggerFactory.getLogger(GeminiFeedbackService.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";


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
        return invocar(PromptsFeedback.deJugador(perfil));
    }

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
                        "parts", List.of(Map.of("text", PromptsFeedback.INSTRUCCION_SISTEMA))),
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
