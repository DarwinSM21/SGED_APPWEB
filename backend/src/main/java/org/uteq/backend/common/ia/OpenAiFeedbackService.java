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
 * Implementacion de {@link GeneradorFeedbackIA} contra la API de OpenAI.
 *
 * <p>Existe porque el nivel gratuito de Gemini resulto poco fiable en la
 * practica: midiendo cinco peticiones identicas seguidas respondieron dos y
 * las otras devolvieron 503 UNAVAILABLE por saturacion del servicio. Un
 * complemento que falla la mitad de las veces no es utilizable delante de un
 * entrenador.
 *
 * <p>Solo se activa con {@code ia.proveedor=openai}. Gemini sigue siendo el
 * predeterminado, asi que este archivo no cambia el comportamiento de nadie
 * que no lo pida explicitamente.
 *
 * <p>La URL base es configurable porque el protocolo de OpenAI se volvio un
 * estandar de hecho: Groq, OpenRouter y NVIDIA NIM exponen el mismo
 * {@code /chat/completions} con el mismo cuerpo. Eso convierte a esta clase en
 * un adaptador para cualquiera de ellos con solo cambiar dos variables de
 * entorno, sin recompilar. Importa porque los niveles gratuitos van y vienen:
 * cuando uno se satura o deja de ser gratis, se cambia de proveedor sin tocar
 * codigo.
 *
 * <p>La clave viaja en la cabecera {@code Authorization}, nunca en la URL: un
 * parametro de consulta termina en logs de acceso, historiales de proxy y
 * cabeceras {@code Referer}.
 */
@Service
@ConditionalOnProperty(name = "ia.proveedor", havingValue = "openai")
public class OpenAiFeedbackService implements GeneradorFeedbackIA {

    private static final Logger log = LoggerFactory.getLogger(OpenAiFeedbackService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;
    private final boolean habilitado;
    private final int reintentos;

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

    /**
     * Reintenta solo ante fallos transitorios. Un 4xx -clave invalida, cuota
     * agotada, cuerpo mal formado- no mejora repitiendo: ahi el problema es
     * nuestro y reintentar solo suma demora delante del usuario.
     */
    private ResultadoFeedback invocar(String prompt) {
        Exception ultimoFallo = null;

        for (int intento = 0; intento <= reintentos; intento++) {
            try {
                return intentarUnaVez(prompt);

            } catch (HttpClientErrorException e) {
                log.warn("OpenAI rechazo la peticion: {}", e.getStatusCode());
                return ResultadoFeedback.noDisponible(motivoDeRechazo(e));

            } catch (Exception e) {
                ultimoFallo = e;
                // Se registra el tipo de fallo, nunca el prompt: aunque va
                // seudonimizado, no hay razon para duplicarlo en los logs.
                log.warn("Fallo transitorio de OpenAI (intento {} de {}): {}",
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

    private ResultadoFeedback intentarUnaVez(String prompt) {
        var cuerpo = Map.of(
                "model", modelo,
                "messages", List.of(
                        Map.of("role", "system", "content", PromptsFeedback.INSTRUCCION_SISTEMA),
                        Map.of("role", "user", "content", prompt)),
                "temperature", 0.4,
                // 300 no alcanzaba. Los modelos de razonamiento -como el
                // gpt-oss que sirve Groq- gastan parte del presupuesto
                // pensando ANTES de escribir, y ese gasto cuenta contra
                // max_tokens: medido con el prompt real, 298 de los 300 se
                // iban en razonamiento y la respuesta llegaba vacia con
                // finish_reason "length". Con 800 quedan ~380 libres para el
                // texto, que es de sobra para las 3 frases que se piden.
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
            // Dos causas distintas y conviene no confundirlas al diagnosticar:
            // "length" es que el presupuesto de tokens se agoto -sube
            // max_tokens-; cualquier otra cosa suele ser el filtro de
            // seguridad del proveedor. Ninguna es transitoria, asi que no se
            // reintenta.
            String motivoCorte = respuesta.path("choices").path(0).path("finish_reason").asText("");
            if ("length".equals(motivoCorte)) {
                log.warn("El modelo agoto el presupuesto de tokens antes de escribir "
                        + "(finish_reason=length); revisar max_tokens");
                return ResultadoFeedback.noDisponible(
                        "El modelo se quedo sin espacio para responder");
            }
            log.warn("OpenAI respondio sin texto utilizable (finish_reason={})", motivoCorte);
            return ResultadoFeedback.noDisponible("El modelo no devolvio texto");
        }
        return ResultadoFeedback.ok(texto.trim());
    }

    /**
     * Traduce el 4xx a algo que el entrenador pueda entender y actuar. Decir
     * "no respondio" ante un 429 es falso -si respondio, y respondio que se
     * acabo la cuota- y manda a buscar el problema en la red, que es donde no
     * esta.
     */
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
