package org.uteq.backend.seguridad.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ApiException;
import org.uteq.backend.seguridad.audit.service.AuditService;
import org.uteq.backend.seguridad.auth.EmailVerificationTokenStore;
import org.uteq.backend.seguridad.auth.mail.EmailVerificationMailer;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Doble opt-in del correo de contacto (RNF-26, cierra el hallazgo H-09 de
 * {@code docs/etica/ETHICS.md}).
 *
 * <p>{@link #sendConfirmation} se llama al dar de alta una persona y cada vez
 * que se le cambia el correo: deja {@code correo_verificado = false} (lo hacen
 * los servicios que editan la persona) y hace llegar un enlace de un solo uso.
 * {@link #confirm} consume el token y marca el correo como verificado. Hasta
 * entonces, {@link PasswordResetService#solicitar} no envía el enlace de
 * restablecimiento a esa dirección.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final String ENLACE_INVALIDO = "El enlace de confirmación es inválido o expiró.";

    private final PersonRepository personaRepository;
    private final EmailVerificationTokenStore tokenStore;
    private final EmailVerificationMailer mailer;
    private final AuditService auditService;

    @Value("${mail.verify-url-base:https://localhost:8443/#/confirmar-correo}")
    private String urlBase;

    @Value("${mail.verify-token-ttl-hours:48}")
    private long ttlHoras;

    /**
     * Emite y entrega un enlace de confirmación para el correo actual de la
     * persona. No cambia {@code correo_verificado}: eso lo hace el servicio que
     * crea o edita la persona antes de llamar aquí.
     *
     * @param persona persona ya persistida (con id y correo)
     */
    public void sendConfirmation(Person persona) {
        if (persona == null || persona.getIdPersona() == null || persona.getCorreo() == null) {
            return;
        }
        String token = generateToken();
        tokenStore.save(persona.getIdPersona(), token, Duration.ofHours(ttlHoras));

        String url = urlBase + "?token=" + token;
        mailer.sendConfirmation(persona.getCorreo(), url);

        auditService.recordEvent("EMAILVERIFY_SOLICITADO", "Persona", persona.getIdPersona(),
                "se emitió un enlace de confirmación de correo");
        log.info("EMAILVERIFY enlace de confirmación emitido para la persona id={}", persona.getIdPersona());
    }

    /**
     * Consume el token del enlace y marca el correo de la persona como
     * verificado.
     *
     * @param token token recibido en el enlace de confirmación
     * @throws ApiException {@code 400} si el token no es válido, expiró o ya se
     *                      usó, o si la persona ya no existe
     */
    @Transactional
    public void confirm(String token) {
        Long idPersona = tokenStore.resolve(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO));

        Person persona = personaRepository.findById(idPersona)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO));

        persona.setCorreoVerificado(true);
        personaRepository.save(persona);
        tokenStore.consume(token);

        auditService.recordEvent("EMAILVERIFY_CONFIRMADO", "Persona", persona.getIdPersona(),
                "confirmó su correo de contacto");
        log.info("EMAILVERIFY correo confirmado para la persona id={}", persona.getIdPersona());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
