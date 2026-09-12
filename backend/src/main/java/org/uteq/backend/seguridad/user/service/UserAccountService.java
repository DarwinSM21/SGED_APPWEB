package org.uteq.backend.seguridad.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;
import org.uteq.backend.seguridad.auth.PasswordPolicy;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.role.entity.Role;
import org.uteq.backend.seguridad.role.repository.RoleRepository;
import org.uteq.backend.seguridad.user.dto.UserAccountPageResponse;
import org.uteq.backend.seguridad.user.dto.UserAccountRequest;
import org.uteq.backend.seguridad.user.dto.UserAccountResponse;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

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
public class UserAccountService {
    private final UserAccountRepository usuarioRepository;
    private final PersonRepository personaRepository;
    private final GeneralStatusRepository estadoGeneralRepository;
    private final RoleRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final CoachRepository coachRepository;
    private final GuardianRepository representanteRepository;
    private final StudentRepository estudianteRepository;

    /**
     * Lista paginada de todas las cuentas (activas e inactivas).
     *
     * @param pageable paginación y orden
     * @return la página solicitada, envuelta en {@link UserAccountPageResponse}
     */
    @Cacheable(value = RedisCacheConfig.CACHE_USERS, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public UserAccountPageResponse<UserAccountResponse> list(Pageable pageable) {
        Page<UserAccount> page = usuarioRepository.findAll(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new UserAccountPageResponse<>(
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
     * @throws ResourceNotFoundException si no existe o está inactivada
     */
    @Transactional(readOnly = true)
    public UserAccountResponse findById(Long id) {
        UserAccount u = usuarioRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
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
     * @throws ResourceNotFoundException si la persona o el estado no existen
     */
    // linkExistingRecord puede mutar Student/Coach (Representante
    // no tiene caché propia todavía): sin evictar esas listas quedarían con el
    // dato viejo —sin cuenta vinculada— hasta que expire el TTL.
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_USERS, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_COACHES, allEntries = true),
    })
    @Audited(action = "CREAR", entity = "Usuario", idSpel = "#result.idUsuario",
            descriptionSpel = "'creó la cuenta ' + #result.username + ' (' + #result.nombrePersona + ' ' + #result.apellidoPersona + ')'")
    @Transactional
    public UserAccountResponse create(UserAccountRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        passwordPolicy.validate(request.password(), request.username());
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya se encuentra registrado");
        }

        Person persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con id: " + request.idPersona()));

        GeneralStatus estado = estadoGeneralRepository.findById(request.idEstadoGeneral())
                .orElseThrow(() -> new ResourceNotFoundException("Estado general no encontrado con id: " + request.idEstadoGeneral()));

        UserAccount.UserAccountBuilder builder = UserAccount.builder()
                .person(persona)
                .generalStatus(estado)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .active(true);

        if (request.rol() != null) {
            validateRoleCoherent(request.idPersona(), request.rol());
            builder.roles(Set.of(findRole(request.rol())));
        }

        UserAccount usuario = usuarioRepository.save(builder.build());

        if (request.rol() != null) {
            linkExistingRecord(request.idPersona(), request.rol(), usuario);
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
     * @throws ResourceNotFoundException si la cuenta, la persona o el
     *                                      estado no existen
     * @throws IllegalArgumentException     si el {@code username} nuevo ya
     *                                      está ocupado o el rol no es
     *                                      coherente con la ficha
     */
    // linkExistingRecord puede mutar Student/Coach (Representante
    // no tiene caché propia todavía): sin evictar esas listas quedarían con el
    // dato viejo —sin cuenta vinculada— hasta que expire el TTL.
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_USERS, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_COACHES, allEntries = true),
    })
    @Audited(action = "EDITAR", entity = "Usuario", idSpel = "#result.idUsuario",
            descriptionSpel = "'editó la cuenta ' + #result.username + ' (' + #result.nombrePersona + ' ' + #result.apellidoPersona + ')'")
    @Transactional
    public UserAccountResponse update(Long id, UserAccountRequest request) {
        UserAccount usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (!usuario.getUsername().equalsIgnoreCase(request.username())
                && usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya está ocupado");
        }

        Person persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con id: " + request.idPersona()));

        GeneralStatus estado = estadoGeneralRepository.findById(request.idEstadoGeneral())
                .orElseThrow(() -> new ResourceNotFoundException("Estado general no encontrado con id: " + request.idEstadoGeneral()));

        usuario.setPerson(persona);
        usuario.setGeneralStatus(estado);
        usuario.setUsername(request.username());

        updatePasswordIfApplicable(usuario, request.password());
        updateRoleIfChanged(usuario, persona, request.rol());

        usuario = usuarioRepository.save(usuario);

        if (request.rol() != null) {
            linkExistingRecord(persona.getId(), request.rol(), usuario);
        }

        return toResponse(usuario);
    }

    /**
     * Baja lógica de una cuenta ({@code activo = false}).
     *
     * @param id identificador de la cuenta
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(action = "ELIMINAR", entity = "Usuario", idSpel = "#p0",
            descriptionSpel = "'desactivó la cuenta de usuario #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_USERS, allEntries = true)
    @Transactional
    public void delete(Long id) {
        UserAccount usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        usuario.setActive(false);
        usuarioRepository.save(usuario);
    }

    /**
     * Reactiva una cuenta dada de baja ({@code activo = true}).
     *
     * @param id identificador de la cuenta
     * @return la cuenta reactivada
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si la cuenta ya está activa
     */
    @Audited(action = "REACTIVAR", entity = "Usuario", idSpel = "#p0",
            descriptionSpel = "'reactivo la cuenta de usuario #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_USERS, allEntries = true)
    @Transactional
    public UserAccountResponse reactivate(Long id) {
        UserAccount usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (Boolean.TRUE.equals(usuario.getActive())) {
            throw new IllegalArgumentException("La cuenta ya se encuentra activa");
        }

        usuario.setActive(true);
        return toResponse(usuarioRepository.save(usuario));
    }

    // R-09 (informe de evaluación de calidad): extraído de update() para bajar
    // su complejidad ciclomática. "password en blanco" significa "no cambiarla".
    private void updatePasswordIfApplicable(UserAccount usuario, String nuevaPassword) {
        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            passwordPolicy.validate(nuevaPassword, usuario.getUsername());
            usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        }
    }

    // R-09: ídem. Solo revalida y reasigna el rol si de verdad cambió; si el
    // rol pedido es null (el formulario de edición no toca roles) o es el
    // mismo que ya tiene, no hace nada.
    private void updateRoleIfChanged(UserAccount usuario, Person persona, String rolPedido) {
        if (rolPedido == null) {
            return;
        }
        String rolActual = usuario.getRoles() == null ? null
                : usuario.getRoles().stream().findFirst().map(Role::getName).orElse(null);
        if (!rolPedido.equals(rolActual)) {
            validateRoleCoherent(persona.getId(), rolPedido);
            // HashSet mutable: Hibernate necesita poder mutar la colección ya
            // administrada de este Usuario persistido. Set.of() es inmutable y
            // hace fallar el flush con UnsupportedOperationException.
            usuario.setRoles(new java.util.HashSet<>(Set.of(findRole(rolPedido))));
        }
    }

    private Role findRole(String nombre) {
        return rolRepository.findByName(nombre)
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
    private void validateRoleCoherent(Long idPersona, String rol) {
        if (estudianteRepository.existsByPerson_IdAndActiveTrue(idPersona)
                && !"ESTUDIANTE".equals(rol)) {
            throw new IllegalArgumentException(
                    "La persona tiene una ficha de estudiante activa: su cuenta solo puede tener el rol ESTUDIANTE");
        }
        if (coachRepository.existsByPerson_IdAndActiveTrue(idPersona)
                && !"ENTRENADOR".equals(rol)) {
            throw new IllegalArgumentException(
                    "La persona tiene una ficha de entrenador activa: su cuenta solo puede tener el rol ENTRENADOR");
        }
        if (representanteRepository.existsByPerson_IdAndActiveTrue(idPersona)
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
    private void linkExistingRecord(Long idPersona, String rol, UserAccount usuario) {
        switch (rol) {
            case "ESTUDIANTE" -> estudianteRepository.findByPerson_IdAndActiveTrue(idPersona)
                    .filter(e -> e.getUserAccount() == null)
                    .ifPresent(e -> { e.setUserAccount(usuario); estudianteRepository.save(e); });
            case "ENTRENADOR" -> coachRepository.findByPerson_IdAndActiveTrue(idPersona)
                    .filter(e -> e.getUsuario() == null)
                    .ifPresent(e -> { e.setUsuario(usuario); coachRepository.save(e); });
            case "REPRESENTANTE" -> representanteRepository.findByPerson_IdAndActiveTrue(idPersona)
                    .filter(r -> r.getUserAccount() == null)
                    .ifPresent(r -> { r.setUserAccount(usuario); representanteRepository.save(r); });
            default -> { }
        }
    }

    private UserAccountResponse toResponse(UserAccount u) {
        List<String> roles = u.getRoles() == null ? List.of()
                : u.getRoles().stream().map(Role::getName).toList();
        return new UserAccountResponse(
                u.getId(),
                u.getPerson().getId(),
                u.getPerson().getName(),
                u.getPerson().getLastName(),
                u.getPerson().getEmail(),
                u.getGeneralStatus().getId(),
                u.getGeneralStatus().getName(),
                u.getUsername(),
                roles,
                u.getLastAccess(),
                u.getActive(),
                u.getCreatedAt()
        );
    }
}
