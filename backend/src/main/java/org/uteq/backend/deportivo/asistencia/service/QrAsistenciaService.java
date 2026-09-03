package org.uteq.backend.deportivo.asistencia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Emisión y validación de los códigos QR con los que los estudiantes marcan
 * su propia asistencia.
 *
 * <p>La pantalla de recepción pide un token, lo pinta como QR y lo renueva
 * cada pocos segundos. Un QR fijo se fotografía una vez y se comparte: al
 * rotar, una captura sirve unos segundos y después no vale nada.
 *
 * <p>En el QR no viaja ningún dato personal, solo un identificador opaco y
 * aleatorio; la identidad de quien marca sale de su propia sesión. Los
 * tokens viven en Redis con expiración automática y son de un solo uso; lo
 * que queda auditado, en {@code deportivo.asistencias.metodo}, es que la
 * marca se hizo por QR.
 */
@Service
@RequiredArgsConstructor
public class QrAsistenciaService {
    private static final String PREFIJO = "qr:asistencia:";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final StringRedisTemplate redis;

    /**
     * Ventana de validez del token. Debe cubrir con holgura lo que tarda un
     * estudiante en enfocar, sin dar tiempo a difundir una captura. El valor
     * por defecto es el doble del período de rotación de la pantalla.
     */
    @Value("${asistencia.qr.ttl-segundos:60}")
    private int ttlSegundos;

    /**
     * Genera un token nuevo para una sesión de entrenamiento.
     *
     * @param idSesion sesión a la que se asociará el token
     * @return el token y sus segundos de validez, para que la pantalla lo
     *         codifique como QR
     */
    public TokenQr emitir(Long idSesion) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redis.opsForValue().set(PREFIJO + token,
                String.valueOf(idSesion),
                Duration.ofSeconds(ttlSegundos));

        return new TokenQr(token, ttlSegundos);
    }

    /**
     * Canjea un token: devuelve la sesión a la que corresponde y lo invalida
     * en el mismo paso. El consumo es atómico ({@code getAndDelete}), así dos
     * peticiones concurrentes con el mismo token no pasan ambas.
     *
     * @param token token escaneado; puede ser {@code null} o en blanco
     * @return la sesión, o {@link Optional#empty()} si el token no existe, ya
     *         se usó o está mal formado
     */
    public Optional<Long> canjear(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String idSesion = redis.opsForValue().getAndDelete(PREFIJO + token);
        if (idSesion == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(idSesion));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Token vigente y cuántos segundos le quedan, para que la pantalla sepa
     * cuándo volver a pedirlo.
     *
     * @param token             valor a codificar como QR
     * @param expiraEnSegundos  segundos restantes de validez
     */
    public record TokenQr(String token, int expiraEnSegundos) {}
}
