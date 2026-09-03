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
import org.uteq.backend.seguridad.auditoria.service.AuditoriaService;
import org.uteq.backend.seguridad.auth.dto.LoginRequest;
import org.uteq.backend.seguridad.auth.dto.RegisterRequest;
import org.uteq.backend.seguridad.auth.dto.SesionResponse;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.LoginAttemptService;
import org.uteq.backend.seguridad.auth.security.RedisBlacklistService;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

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
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final EstadoGeneralRepository estadoGeneralRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    /**
     * Resultado de un inicio de sesión correcto: los dos tokens que el
     * controlador coloca en cookies {@code HttpOnly} y la vista de sesión
     * que devuelve en el cuerpo.
     *
     * @param accessToken  JWT de acceso, de vida corta
     * @param refreshToken JWT de refresco, de vida larga
     * @param sesion       datos no sensibles de la sesión (nunca el token)
     */
    public record LoginResult(String accessToken, String refreshToken, SesionResponse sesion) {}

    /**
     * Da de alta una {@link Persona} y su {@link Usuario} en una sola
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
     * @throws IllegalArgumentException si {@code request.rol()} no existe en
     *                                  el catálogo de roles
     * @throws IllegalStateException    si falta el catálogo
     *                                  {@code seguridad.estados_general}
     */
    @Transactional
    public Optional<SesionResponse> registrar(RegisterRequest request) {
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())
                || personaRepository.existsByCedulaAndActivoTrue(request.cedula())
                || personaRepository.existsByCorreo(request.correo())) {
            return Optional.empty();
        }

        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new IllegalArgumentException("Rol inexistente: " + request.rol()));

        Persona persona = Persona.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .cedula(request.cedula())
                .correo(request.correo())
                .fechaNacimiento(request.fechaNacimiento())
                .activo(true)
                .build();
        persona = personaRepository.save(persona);

        // id_estado_general es NOT NULL: sin esto el alta también falla en base
        // de datos aunque la persona ya se haya podido insertar.
        EstadoGeneral estadoActivo = estadoGeneralRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el catalogo seguridad.estados_general (ver db/seed.sql)"));

        Usuario usuario = Usuario.builder()
                .persona(persona)
                .estadoGeneral(estadoActivo)
                .username(request.username())
                .password_Hash(passwordEncoder.encode(request.password()))
                .activo(true)
                .roles(Set.of(rol))
                .build();
        usuario = usuarioRepository.save(usuario);

        String nombreCompleto = persona.getNombre() + " " + persona.getApellido();
        return Optional.of(SesionResponse.builder()
                .username(usuario.getUsername())
                .nombre(nombreCompleto)
                .rol(rol.getNombre())
                .idPersona(persona.getIdPersona())
                .idUsuario(usuario.getIdUsuario())
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
        if (loginAttemptService.estaBloqueada(ip)) {
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Intenta de nuevo en 15 minutos.");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.registrarFallo(ip);
            AUTH_AUDIT_LOG.warn("AUTH_LOGIN_FAIL ip={} sub={}", ip, request.username());
            auditoriaService.registrarConIdentidad(request.username(), null,
                    "LOGIN_FALLIDO", "Usuario", null, "intento de inicio de sesión fallido");
            throw e;
        }

        loginAttemptService.registrarExito(ip);

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String rol = userDetails.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
        AUTH_AUDIT_LOG.info("AUTH_LOGIN_OK ip={} sub={}", ip, userDetails.getUsername());
        auditoriaService.registrarConIdentidad(userDetails.getUsername(), rol,
                "LOGIN", "Usuario", null, "inició sesión");

        String accessToken = jwtService.generateToken(userDetails.getUsername(), rol);
        String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername(), rol);

        String nombre = usuarioRepository.findByUsernameAndActivoTrue(userDetails.getUsername())
                .map(u -> u.getPersona().getNombre() + " " + u.getPersona().getApellido())
                .orElse(userDetails.getUsername());

        SesionResponse sesion = SesionResponse.builder()
                .username(userDetails.getUsername())
                .nombre(nombre)
                .rol(rol)
                .build();

        return new LoginResult(accessToken, refreshToken, sesion);
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
                    blacklistService.revocar(jti, jwtService.getExpirationMs());
                }
            } catch (Exception e) {
                // Token ya inválido: nada que revocar.
            }
        }

        auditoriaService.registrar("LOGOUT", "Usuario", null, "cerró sesión");
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
    public Optional<String> refrescar(String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            return Optional.empty();
        }

        String username = jwtService.extractUsername(refreshToken);
        String rol = jwtService.extractRol(refreshToken);
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
    public Optional<SesionResponse> obtenerSesionActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            return Optional.empty();
        }

        String rol = userDetails.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");

        String nombre = usuarioRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.getPersona().getNombre() + " " + u.getPersona().getApellido())
                .orElse(userDetails.getUsername());

        return Optional.of(SesionResponse.builder()
                .username(userDetails.getUsername())
                .nombre(nombre)
                .rol(rol)
                .build());
    }
}
