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
 * Emision y validacion de los codigos QR con los que los estudiantes marcan su
 * propia asistencia.
 *
 * <p><b>Como funciona.</b> La pantalla de recepcion pide un token, lo pinta
 * como QR y lo renueva cada pocos segundos. El estudiante lo escanea con su
 * celular y su aplicacion lo envia junto con su sesion autenticada. El backend
 * comprueba que el token siga vigente y registra la asistencia.
 *
 * <p><b>Por que rota.</b> Un QR fijo se fotografia una vez y se comparte por
 * WhatsApp: cualquiera marcaria asistencia desde su casa, que es exactamente
 * lo que el mecanismo quiere impedir. Al rotar, una captura de pantalla sirve
 * unos segundos y despues no vale nada. Es el mismo principio de los codigos
 * de un solo uso de la autenticacion en dos pasos.
 *
 * <p><b>Que NO viaja en el QR.</b> Ni el nombre del estudiante, ni su codigo,
 * ni ningun dato personal: solo un identificador opaco y aleatorio. La
 * identidad de quien marca sale de su propia sesion, nunca del codigo. Esto
 * es deliberado: si cada estudiante tuviera su QR personal, ese codigo lo
 * identificaria y bastaria una foto para suplantarlo.
 *
 * <p>Los tokens viven en Redis con expiracion automatica. No se guardan en la
 * base de datos porque son efimeros y de un solo uso; lo que si queda
 * auditado, en {@code deportivo.asistencias.metodo}, es que la marca se hizo
 * por QR.
 */
@Service
@RequiredArgsConstructor
public class QrAsistenciaService {

    private static final String PREFIJO = "qr:asistencia:";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final StringRedisTemplate redis;

    /**
     * Ventana de validez del token. Debe cubrir con holgura lo que tarda un
     * estudiante en sacar el celular y enfocar, sin dar tiempo a difundir una
     * captura. El valor por defecto es el doble del periodo de rotacion de la
     * pantalla, para que un token generado justo antes de un refresco siga
     * siendo valido mientras alguien lo escanea.
     */
    @Value("${asistencia.qr.ttl-segundos:60}")
    private int ttlSegundos;

    /**
     * Genera un token nuevo para una sesion de entrenamiento.
     *
     * @return el valor que la pantalla de recepcion debe codificar como QR
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
     * Canjea un token. Devuelve la sesion a la que corresponde y lo invalida
     * en el mismo paso.
     *
     * <p>El consumo es atomico: {@code getAndDelete} evita que dos peticiones
     * concurrentes con el mismo token pasen ambas la validacion. Aun asi, la
     * restriccion {@code uq_asistencia_sesion_estudiante} de la base es la
     * garantia final de que nadie quede registrado dos veces.
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
     * Token vigente y cuantos segundos le quedan, para que la pantalla sepa
     * cuando volver a pedirlo.
     */
    public record TokenQr(String token, int expiraEnSegundos) {}
}
