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
 * Logica de negocio de autenticacion, antes embebida en AuthController
 * (hallazgo D-03 del informe de evaluacion de calidad: 278 lineas de
 * controlador con 4 repositorios inyectados y reglas de negocio que no
 * podian probarse sin levantar el contexto HTTP). El controlador conserva
 * solo la traduccion HTTP: cookies, codigos de estado y el cuerpo de la
 * respuesta.
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

    public record LoginResult(String accessToken, String refreshToken, SesionResponse sesion) {}

    /**
     * rol es obligatorio (@NotBlank en RegisterRequest): no existe un rol
     * generico al que caer por defecto. Quien llama ya es ADMINISTRADOR, asi
     * que puede pedir cualquier rol existente en seguridad.roles (p.ej.
     * ENTRENADOR, RECEPCIONISTA, REPRESENTANTE) - un nombre que no exista
     * responde 400 via RolRepository.findByNombre.
     *
     * @Transactional: antes guardaba Persona y Usuario en dos pasos sueltos,
     * sin transaccion propia. Con un tercer guardado (la fila de dominio de
     * Entrenador/Representante) encadenado desde el frontend justo despues,
     * una falla a mitad de camino dejaria una Persona sin Usuario o un
     * Usuario sin rol asignado.
     *
     * @return vacio si el username, la cedula o el correo ya existen
     *         (el controlador lo traduce a 409 Conflict).
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

        // id_estado_general es NOT NULL: sin esto el alta tambien falla en base
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
     * @throws TooManyRequestsException si la IP esta bloqueada por intentos fallidos.
     * @throws BadCredentialsException si las credenciales son incorrectas.
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

    public void logout(String accessToken) {
        if (accessToken != null) {
            try {
                String jti = jwtService.extractJti(accessToken);
                if (jti != null) {
                    blacklistService.revocar(jti, jwtService.getExpirationMs());
                }
            } catch (Exception e) {
                // Token ya invalido, ignorar
            }
        }

        auditoriaService.registrar("LOGOUT", "Usuario", null, "cerró sesión");
        SecurityContextHolder.clearContext();
    }

    /** @return vacio si el refresh token falta o no es valido. */
    public Optional<String> refrescar(String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            return Optional.empty();
        }

        String username = jwtService.extractUsername(refreshToken);
        String rol = jwtService.extractRol(refreshToken);
        return Optional.of(jwtService.generateToken(username, rol));
    }

    /** @return vacio si no hay una sesion autenticada en el contexto actual. */
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
