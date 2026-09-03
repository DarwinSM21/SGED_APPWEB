package org.uteq.backend.seguridad.usuario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.uteq.backend.seguridad.usuario.dto.UsuarioPageResponse;
import org.uteq.backend.seguridad.usuario.dto.UsuarioRequest;
import org.uteq.backend.seguridad.usuario.dto.UsuarioResponse;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.List;
import java.util.Set;

/**
 * Lógica de negocio de las cuentas de usuario: alta, edición, baja y
 * reactivación, con la particularidad de que el rol de la cuenta debe ser
 * coherente con la ficha de dominio (estudiante / entrenador / representante)
 * que la persona ya tenga activa. Cruza a los repositorios de esos tres
 * dominios solo para leer esa coherencia y para revincular una ficha que se
 * creó antes que la cuenta.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final EstadoGeneralRepository estadoGeneralRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntrenadorRepository entrenadorRepository;
    private final RepresentanteRepository representanteRepository;
    private final EstudianteRepository estudianteRepository;

    /**
     * Lista paginada de todas las cuentas (activas e inactivas).
     *
     * @param pageable paginación y orden
     * @return la página solicitada, envuelta en {@link UsuarioPageResponse}
     */
    @Cacheable(value = RedisCacheConfig.CACHE_USUARIOS, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public UsuarioPageResponse<UsuarioResponse> listar(Pageable pageable) {
        Page<Usuario> page = usuarioRepository.findAll(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new UsuarioPageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Busca una cuenta activa por su identificador.
     *
     * @param id identificador de la cuenta
     * @return la cuenta encontrada
     * @throws RecursoNoEncontradoException si no existe o está inactivada
     */
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario u = usuarioRepository.findByIdUsuarioAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
        return toResponse(u);
 }

    /**
     * Crea una cuenta para una persona ya registrada. Si se indica rol, se
     * valida contra la ficha de dominio activa de la persona y, tras guardar,
     * se revincula esa ficha si estaba sin cuenta.
     *
     * @param request datos de la cuenta ({@code idPersona}, {@code username},
     *                {@code password}, {@code idEstadoGeneral}, {@code rol}
     *                opcional)
     * @return la cuenta creada
     * @throws IllegalArgumentException     si falta la contraseña, si el
     *                                      {@code username} ya existe o si el
     *                                      rol no es coherente con la ficha
     * @throws RecursoNoEncontradoException si la persona o el estado no existen
     */
    // vincularFichaExistente puede mutar Estudiante/Entrenador (Representante
    // no tiene caché propia todavía): sin evictar esas listas quedarían con el
    // dato viejo —sin cuenta vinculada— hasta que expire el TTL.
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_USUARIOS, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_ENTRENADORES, allEntries = true),
    })
    @Auditado(accion = "CREAR", entidad = "Usuario", idSpel = "#result.idUsuario",
            descripcionSpel = "'creó la cuenta ' + #result.username + ' (' + #result.nombrePersona + ' ' + #result.apellidoPersona + ')'")
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya se encuentra registrado");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con id: " + request.idPersona()));

        EstadoGeneral estado = estadoGeneralRepository.findById(request.idEstadoGeneral())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado general no encontrado con id: " + request.idEstadoGeneral()));

        Usuario.UsuarioBuilder builder = Usuario.builder()
                .persona(persona)
                .estadoGeneral(estado)
                .username(request.username())
                .password_Hash(passwordEncoder.encode(request.password()))
                .activo(true);

        if (request.rol() != null) {
            validarRolCoherente(request.idPersona(), request.rol());
            builder.roles(Set.of(buscarRol(request.rol())));
        }

        Usuario usuario = usuarioRepository.save(builder.build());

        if (request.rol() != null) {
            vincularFichaExistente(request.idPersona(), request.rol(), usuario);
        }

        return toResponse(usuario);
    }

    /**
     * Actualiza una cuenta. La contraseña solo cambia si {@code request}
     * trae una no vacía; el rol solo se revalida y reasigna si de verdad
     * cambió.
     *
     * @param id      identificador de la cuenta a editar
     * @param request datos nuevos
     * @return la cuenta actualizada
     * @throws RecursoNoEncontradoException si la cuenta, la persona o el
     *                                      estado no existen
     * @throws IllegalArgumentException     si el {@code username} nuevo ya
     *                                      está ocupado o el rol no es
     *                                      coherente con la ficha
     */
    // vincularFichaExistente puede mutar Estudiante/Entrenador (Representante
    // no tiene caché propia todavía): sin evictar esas listas quedarían con el
    // dato viejo —sin cuenta vinculada— hasta que expire el TTL.
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_USUARIOS, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_ENTRENADORES, allEntries = true),
    })
    @Auditado(accion = "EDITAR", entidad = "Usuario", idSpel = "#result.idUsuario",
            descripcionSpel = "'editó la cuenta ' + #result.username + ' (' + #result.nombrePersona + ' ' + #result.apellidoPersona + ')'")
    @Transactional
    public UsuarioResponse editar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));

        if (!usuario.getUsername().equalsIgnoreCase(request.username())
                && usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya está ocupado");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con id: " + request.idPersona()));

        EstadoGeneral estado = estadoGeneralRepository.findById(request.idEstadoGeneral())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado general no encontrado con id: " + request.idEstadoGeneral()));

        usuario.setPersona(persona);
        usuario.setEstadoGeneral(estado);
        usuario.setUsername(request.username());

        actualizarPasswordSiCorresponde(usuario, request.password());
        actualizarRolSiCambio(usuario, persona, request.rol());

        usuario = usuarioRepository.save(usuario);

        if (request.rol() != null) {
            vincularFichaExistente(persona.getIdPersona(), request.rol(), usuario);
        }

        return toResponse(usuario);
    }

    /**
     * Baja lógica de una cuenta ({@code activo = false}).
     *
     * @param id identificador de la cuenta
     * @throws RecursoNoEncontradoException si no existe
     */
    @Auditado(accion = "ELIMINAR", entidad = "Usuario", idSpel = "#p0",
            descripcionSpel = "'desactivó la cuenta de usuario #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_USUARIOS, allEntries = true)
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    /**
     * Reactiva una cuenta dada de baja ({@code activo = true}).
     *
     * @param id identificador de la cuenta
     * @return la cuenta reactivada
     * @throws RecursoNoEncontradoException si no existe
     * @throws IllegalArgumentException     si la cuenta ya está activa
     */
    @Auditado(accion = "REACTIVAR", entidad = "Usuario", idSpel = "#p0",
            descripcionSpel = "'reactivo la cuenta de usuario #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_USUARIOS, allEntries = true)
    @Transactional
    public UsuarioResponse reactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("La cuenta ya se encuentra activa");
        }

        usuario.setActivo(true);
        return toResponse(usuarioRepository.save(usuario));
    }

    // R-09 (informe de evaluación de calidad): extraído de editar() para bajar
    // su complejidad ciclomática. "password en blanco" significa "no cambiarla".
    private void actualizarPasswordSiCorresponde(Usuario usuario, String nuevaPassword) {
        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            usuario.setPassword_Hash(passwordEncoder.encode(nuevaPassword));
        }
    }

    // R-09: ídem. Solo revalida y reasigna el rol si de verdad cambió; si el
    // rol pedido es null (el formulario de edición no toca roles) o es el
    // mismo que ya tiene, no hace nada.
    private void actualizarRolSiCambio(Usuario usuario, Persona persona, String rolPedido) {
        if (rolPedido == null) {
            return;
        }
        String rolActual = usuario.getRoles() == null ? null
                : usuario.getRoles().stream().findFirst().map(Rol::getNombre).orElse(null);
        if (!rolPedido.equals(rolActual)) {
            validarRolCoherente(persona.getIdPersona(), rolPedido);
            // HashSet mutable: Hibernate necesita poder mutar la colección ya
            // administrada de este Usuario persistido. Set.of() es inmutable y
            // hace fallar el flush con UnsupportedOperationException.
            usuario.setRoles(new java.util.HashSet<>(Set.of(buscarRol(rolPedido))));
        }
    }

    private Rol buscarRol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("Rol inexistente: " + nombre));
    }

    /**
     * El rol de la cuenta debe coincidir con la ficha de dominio que ya tiene
     * la persona: un estudiante no puede tener una cuenta de entrenador. Solo
     * pesan las fichas activas —si a alguien le dieron de baja su ficha de
     * entrenador, queda libre para tomar otro rol—. Una persona sin ninguna
     * ficha acepta cualquier rol: es necesario, porque el alta de un
     * entrenador crea primero la cuenta con rol {@code ENTRENADOR} y después
     * la ficha.
     *
     * @param idPersona persona cuya coherencia se valida
     * @param rol       rol que se le quiere asignar a la cuenta
     * @throws IllegalArgumentException si la persona tiene una ficha activa de
     *                                  otro rol
     */
    private void validarRolCoherente(Long idPersona, String rol) {
        if (estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(idPersona)
                && !"ESTUDIANTE".equals(rol)) {
            throw new IllegalArgumentException(
                    "La persona tiene una ficha de estudiante activa: su cuenta solo puede tener el rol ESTUDIANTE");
        }
        if (entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(idPersona)
                && !"ENTRENADOR".equals(rol)) {
            throw new IllegalArgumentException(
                    "La persona tiene una ficha de entrenador activa: su cuenta solo puede tener el rol ENTRENADOR");
        }
        if (representanteRepository.existsByPersona_IdPersonaAndActivoTrue(idPersona)
                && !"REPRESENTANTE".equals(rol)) {
            throw new IllegalArgumentException(
                    "La persona tiene una ficha de representante activa: su cuenta solo puede tener el rol REPRESENTANTE");
        }
    }

    /**
     * Si la persona ya tenía una ficha de dominio activa creada <em>antes</em>
     * que esta cuenta (caso típico: alumno inscrito primero, acceso habilitado
     * después desde la pantalla de Personas), la ficha quedó con
     * {@code id_usuario} nulo. Sin este respaldo la cuenta inicia sesión pero
     * cada endpoint "propio" (mi historial, marcar asistencia, mis
     * representados) resuelve la ficha por username y no la encuentra.
     *
     * @param idPersona persona dueña de la ficha
     * @param rol       rol de la cuenta, que determina qué ficha buscar
     * @param usuario   cuenta recién guardada a la que vincular la ficha
     */
    private void vincularFichaExistente(Long idPersona, String rol, Usuario usuario) {
        switch (rol) {
            case "ESTUDIANTE" -> estudianteRepository.findByPersona_IdPersonaAndActivoTrue(idPersona)
                    .filter(e -> e.getUsuario() == null)
                    .ifPresent(e -> { e.setUsuario(usuario); estudianteRepository.save(e); });
            case "ENTRENADOR" -> entrenadorRepository.findByPersona_IdPersonaAndActivoTrue(idPersona)
                    .filter(e -> e.getUsuario() == null)
                    .ifPresent(e -> { e.setUsuario(usuario); entrenadorRepository.save(e); });
            case "REPRESENTANTE" -> representanteRepository.findByPersona_IdPersonaAndActivoTrue(idPersona)
                    .filter(r -> r.getUsuario() == null)
                    .ifPresent(r -> { r.setUsuario(usuario); representanteRepository.save(r); });
            default -> { }
        }
    }

    private UsuarioResponse toResponse(Usuario u) {
        List<String> roles = u.getRoles() == null ? List.of()
                : u.getRoles().stream().map(Rol::getNombre).toList();
        return new UsuarioResponse(
                u.getIdUsuario(),
                u.getPersona().getIdPersona(),
                u.getPersona().getNombre(),
                u.getPersona().getApellido(),
                u.getPersona().getCorreo(),
                u.getEstadoGeneral().getIdEstadoGeneral(),
                u.getEstadoGeneral().getNombre(),
                u.getUsername(),
                roles,
                u.getUltimoAcceso(),
                u.getActivo(),
                u.getCreatedAt()
        );
    }
}
