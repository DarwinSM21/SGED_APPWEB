package org.uteq.backend.seguridad.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.TooManyRequestsException;
import org.uteq.backend.seguridad.audit.service.AuditService;
import org.uteq.backend.seguridad.auth.PasswordPolicy;
import org.uteq.backend.seguridad.auth.dto.LoginRequest;
import org.uteq.backend.seguridad.auth.dto.RegisterRequest;
import org.uteq.backend.seguridad.auth.dto.SessionResponse;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.LoginAttemptService;
import org.uteq.backend.seguridad.auth.security.RedisBlacklistService;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.role.entity.Role;
import org.uteq.backend.seguridad.role.repository.RoleRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.util.Optional;
import java.util.Set;

/**
 * Lógica de negocio de autenticación, antes embebida en {@code AuthController}
 * (hallazgo D-03 del informe de evaluación de calidad: un controlador con
 * cuatro repositorios inyectados y reglas de negocio que no podían probarse
 * sin levantar el contexto HTTP). El controlador conserva solo la traducción
 * HTTP: cookies, códigos de estado y el cuerpo de la respuesta.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger AUTH_AUDIT_LOG = LoggerFactory.getLogger("AUTH_AUDIT");

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisBlacklistService blacklistService;
    private final LoginAttemptService loginAttemptService;
    private final UserAccountRepository usuarioRepository;
    private final PersonRepository personaRepository;
    private final RoleRepository rolRepository;
    private final GeneralStatusRepository estadoGeneralRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AuditService auditoriaService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Resultado de un inicio de sesión correcto: los dos tokens que el
     * controlador coloca en cookies {@code HttpOnly} y la vista de sesión
     * que devuelve en el cuerpo.
     *
     * @param accessToken  JWT de acceso, de vida corta
     * @param refreshToken JWT de refresco, de vida larga
     * @param session      datos no sensibles de la sesión (nunca el token)
     */
    public record LoginResult(String accessToken, String refreshToken, SessionResponse session) {}

    /**
     * Da de alta una {@link Person} y su {@link UserAccount} en una sola
     * transacción. El campo {@code rol} es obligatorio ({@code @NotBlank} en
     * {@link RegisterRequest}): no hay un rol genérico por defecto. Quien
     * llama ya es {@code ADMINISTRADOR}, así que puede pedir cualquier rol
     * existente en {@code seguridad.roles}.
     *
     * @param request datos de la persona y de la cuenta a crear; ya validado
     *                por {@code @Valid} en el controlador
     * @return la sesión del usuario recién creado, o {@link Optional#empty()}
     *         si el {@code username}, la cédula o el correo ya están en uso
     *         (el controlador lo traduce a {@code 409 Conflict})
     * @throws org.uteq.backend.common.exception.ApiException {@code 422} si la
     *                                  contraseña no cumple la política (RNF-14)
     * @throws IllegalArgumentException si {@code request.rol()} no existe en
     *                                  el catálogo de roles
     * @throws IllegalStateException    si falta el catálogo
     *                                  {@code seguridad.estados_general}
     */
    @Transactional
    public Optional<SessionResponse> register(RegisterRequest request) {
        passwordPolicy.validate(request.password(), request.username());

        // RF-49 / H-01: la cédula es opcional; solo cuenta como colisión si viene.
        boolean cedulaDuplicada = request.cedula() != null && !request.cedula().isBlank()
                && personaRepository.existsByNationalIdAndActiveTrue(request.cedula());
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())
                || cedulaDuplicada
                || personaRepository.existsByCorreo(request.correo())) {
            return Optional.empty();
        }

        Role rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new IllegalArgumentException("Rol inexistente: " + request.rol()));

        Person persona = Person.builder()
                .name(request.nombre())
                .lastName(request.apellido())
                .nationalId(request.cedula())
                .email(request.correo())
                .birthDate(request.fechaNacimiento())
                .active(true)
                .emailVerified(false)
                .build();
        persona = personaRepository.save(persona);
        // RNF-26 / H-09: doble opt-in del correo recién registrado.
        emailVerificationService.sendConfirmation(persona);

        // id_estado_general es NOT NULL: sin esto el alta también falla en base
        // de datos aunque la persona ya se haya podido insertar.
        GeneralStatus estadoActivo = estadoGeneralRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el catalogo seguridad.estados_general (ver db/seed.sql)"));

        UserAccount usuario = UserAccount.builder()
                .person(persona)
                .generalStatus(estadoActivo)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .active(true)
                .roles(Set.of(rol))
                .build();
        usuario = usuarioRepository.save(usuario);

        String nombreCompleto = persona.getName() + " " + persona.getLastName();
        return Optional.of(SessionResponse.builder()
                .username(usuario.getUsername())
                .nombre(nombreCompleto)
                .rol(rol.getName())
                .idPersona(persona.getId())
                .idUsuario(usuario.getId())
                .build());
    }

    /**
     * Autentica al usuario y emite los tokens de sesión. Un fallo de
     * credenciales cuenta contra el límite por IP ({@code 5 / 15 min}) y
     * queda registrado en auditoría; un acierto reinicia ese contador.
     *
     * @param request credenciales ({@code username} y {@code password})
     * @param ip      dirección remota del cliente, clave del límite de intentos
     * @return los dos tokens más la vista de sesión
     * @throws TooManyRequestsException si la IP está bloqueada por intentos
     *                                  fallidos previos
     * @throws BadCredentialsException  si el usuario o la contraseña no son
     *                                  correctos
     */
    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request, String ip) {
        if (loginAttemptService.isBlocked(ip)) {
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Intenta de nuevo en 15 minutos.");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(ip);
            AUTH_AUDIT_LOG.warn("AUTH_LOGIN_FAIL ip={} sub={}", ip, request.username());
            auditoriaService.recordEventWithIdentity(request.username(), null,
                    "LOGIN_FALLIDO", "Usuario", null, "intento de inicio de sesión fallido");
            throw e;
        }

        loginAttemptService.recordSuccess(ip);

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String rol = userDetails.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
        AUTH_AUDIT_LOG.info("AUTH_LOGIN_OK ip={} sub={}", ip, userDetails.getUsername());
        auditoriaService.recordEventWithIdentity(userDetails.getUsername(), rol,
                "LOGIN", "Usuario", null, "inició sesión");

        String accessToken = jwtService.generateToken(userDetails.getUsername(), rol);
        String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername(), rol);

        String nombre = usuarioRepository.findByUsernameAndActivoTrue(userDetails.getUsername())
                .map(u -> u.getPerson().getName() + " " + u.getPerson().getLastName())
                .orElse(userDetails.getUsername());

        SessionResponse session = SessionResponse.builder()
                .username(userDetails.getUsername())
                .nombre(nombre)
                .rol(rol)
                .build();

        return new LoginResult(accessToken, refreshToken, session);
    }

    /**
     * Cierra la sesión: revoca el JWT por su {@code jti} en la lista negra de
     * Redis (hasta que expire por sí mismo) y limpia el contexto de seguridad.
     * Un token ya inválido o ilegible se ignora en silencio: el efecto
     * deseado —que deje de ser aceptado— ya se cumple.
     *
     * @param accessToken el JWT de acceso tomado de la cookie; puede ser
     *                    {@code null} si la cookie no vino
     */
    public void logout(String accessToken) {
        if (accessToken != null) {
            try {
                String jti = jwtService.extractJti(accessToken);
                if (jti != null) {
                    blacklistService.revoke(jti, jwtService.getExpirationMs());
                }
            } catch (Exception e) {
                // Token ya inválido: nada que revocar.
            }
        }

        auditoriaService.recordEvent("LOGOUT", "Usuario", null, "cerró sesión");
        SecurityContextHolder.clearContext();
    }

    /**
     * Emite un nuevo token de acceso a partir de un refresh token vigente.
     *
     * @param refreshToken el JWT de refresco tomado de la cookie; puede ser
     *                     {@code null}
     * @return el nuevo token de acceso, o {@link Optional#empty()} si el
     *         refresh token falta o no es válido (el controlador lo traduce a
     *         {@code 401})
     */
    public Optional<String> refresh(String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            return Optional.empty();
        }

        String username = jwtService.extractUsername(refreshToken);
        String rol = jwtService.extractRole(refreshToken);
        return Optional.of(jwtService.generateToken(username, rol));
    }

    /**
     * Reconstruye la vista de sesión del usuario autenticado en el contexto
     * de seguridad actual.
     *
     * @return los datos de sesión, o {@link Optional#empty()} si no hay una
     *         sesión autenticada en el contexto
     */
    @Transactional(readOnly = true)
    public Optional<SessionResponse> getCurrentSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            return Optional.empty();
        }

        String rol = userDetails.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");

        String nombre = usuarioRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.getPerson().getName() + " " + u.getPerson().getLastName())
                .orElse(userDetails.getUsername());

        return Optional.of(SessionResponse.builder()
                .username(userDetails.getUsername())
                .nombre(nombre)
                .rol(rol)
                .build());
    }
}
