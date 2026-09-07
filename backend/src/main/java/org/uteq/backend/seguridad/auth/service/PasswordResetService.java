package org.uteq.backend.seguridad.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ApiException;
import org.uteq.backend.seguridad.audit.service.AuditService;
import org.uteq.backend.seguridad.auth.PasswordPolicy;
import org.uteq.backend.seguridad.auth.PasswordResetTokenStore;
import org.uteq.backend.seguridad.auth.mail.PasswordResetMailer;
import org.uteq.backend.seguridad.auth.security.SessionEpochService;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Flujo de restablecimiento de contraseña por enlace de un solo uso (RF-37).
 *
 * <p>{@link #solicitar} nunca revela si la cuenta existe: haga lo que haga por
 * dentro, el controlador responde siempre {@code 202}. {@link #restablecer}
 * consume el token, cambia la contraseña, invalida las sesiones abiertas del
 * usuario (vía {@link SessionEpochService}) y audita el cambio.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final String ENLACE_INVALIDO = "El enlace de restablecimiento es inválido o expiró.";

    private final UserAccountRepository usuarioRepository;
    private final PersonRepository personaRepository;
    private final PasswordResetTokenStore tokenStore;
    private final PasswordResetMailer mailer;
    private final SessionEpochService sessionEpochService;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${mail.reset-url-base}")
    private String urlBase;

    @Value("${mail.reset-token-ttl-minutes:30}")
    private long ttlMinutos;

    /**
     * Genera y envía un enlace de restablecimiento si el identificador
     * corresponde a una cuenta activa. Si no, no hace nada: el llamador
     * responde igual en ambos casos.
     *
     * @param identificador username o correo registrado
     */
    @Transactional
    public void solicitar(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return;
        }
        String id = identificador.trim();

        Optional<UserAccount> cuenta = usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue(id)
                .or(() -> personaRepository.findByCorreo(id)
                        .flatMap(p -> usuarioRepository.findByPersona_IdPersonaAndActivoTrue(p.getIdPersona())));
        if (cuenta.isEmpty()) {
            return;
        }

        UserAccount usuario = cuenta.get();
        String token = generarToken();
        tokenStore.guardar(usuario.getUsername(), token, Duration.ofMinutes(ttlMinutos));

        String url = urlBase + "?token=" + token;
        mailer.enviarEnlace(usuario.getPersona().getCorreo(), url);

        auditService.recordEvent("PWRESET_SOLICITADO", "Usuario", usuario.getIdUsuario(),
                "solicitó un enlace de restablecimiento de contraseña");
        log.info("PWRESET enlace generado para el usuario id={}", usuario.getIdUsuario());
    }

    /**
     * Consume el token del enlace y fija la contraseña nueva.
     *
     * @param token         token recibido en el enlace
     * @param nuevaPassword contraseña elegida por el usuario
     * @throws ApiException {@code 400} si el token no es válido, expiró o ya
     *                      se usó; {@code 422} si la contraseña incumple la
     *                      política (RNF-14)
     */
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        String username = tokenStore.resolver(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO));

        passwordPolicy.validar(nuevaPassword, username);

        UserAccount usuario = usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue(username)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO));

        usuario.setPassword_Hash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        tokenStore.consumir(token);
        sessionEpochService.marcar(username);

        auditService.recordEvent("PWRESET_COMPLETADO", "Usuario", usuario.getIdUsuario(),
                "restableció su contraseña mediante enlace");
        log.info("PWRESET contrasena restablecida para el usuario id={}", usuario.getIdUsuario());
    }

    private String generarToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
