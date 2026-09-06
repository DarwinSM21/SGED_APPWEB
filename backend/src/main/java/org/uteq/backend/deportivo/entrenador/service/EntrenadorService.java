package org.uteq.backend.deportivo.entrenador.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorPageResponse;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorRequest;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorResponse;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.especialidad.entity.Especialidad;
import org.uteq.backend.deportivo.especialidad.repository.EspecialidadRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;

/**
 * Lógica de negocio de {@code Entrenador}. Cada entrenador se apoya en una
 * {@code Person} y una cuenta de rol {@code ENTRENADOR} ya creadas; la
 * especialidad es opcional. Las bajas son lógicas ({@code activo = false}).
 */
@Service
@RequiredArgsConstructor
public class EntrenadorService {

    private final EntrenadorRepository entrenadorRepository;
    private final PersonRepository personaRepository;
    private final UserAccountRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;

    /**
     * Lista paginada de entrenadores.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, envuelta en {@link EntrenadorPageResponse}
     */
    @Cacheable(value = RedisCacheConfig.CACHE_COACHES, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public EntrenadorPageResponse<EntrenadorResponse> listar(Pageable pageable) {
        Page<Entrenador> page = entrenadorRepository.findAll(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new EntrenadorPageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Busca un entrenador por su identificador.
     *
     * @param id identificador del entrenador
     * @return el entrenador encontrado
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public EntrenadorResponse buscarPorId(Long id) {
        Entrenador e = entrenadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado con id: " + id));
        return toResponse(e);
    }

    /**
     * Registra un entrenador sobre una persona y una cuenta ya existentes.
     *
     * @param request persona, usuario, especialidad y datos profesionales
     * @return el entrenador registrado
     * @throws ResourceNotFoundException si la persona, el usuario o la
     *                                      especialidad no existen
     * @throws IllegalArgumentException     si la persona o el usuario ya
     *                                      están asignados, o si el usuario
     *                                      no tiene rol {@code ENTRENADOR}
     */
    @CacheEvict(value = RedisCacheConfig.CACHE_COACHES, allEntries = true)
    @Transactional
    public EntrenadorResponse crear(EntrenadorRequest request) {
        if (entrenadorRepository.existsByPersona_IdPersona(request.idPersona())) {
            throw new IllegalArgumentException("La persona ya está registrada como entrenador");
        }
        if (entrenadorRepository.existsByUsuario_IdUsuario(request.idUsuario())) {
            throw new IllegalArgumentException("El usuario ya está asignado a otro entrenador");
        }

        Person persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con id: " + request.idPersona()));

        UserAccount usuario = usuarioRepository.findById(request.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + request.idUsuario()));

        boolean tieneRolEntrenador = usuario.getRoles().stream()
                .anyMatch(r -> "ENTRENADOR".equals(r.getNombre()));
        if (!tieneRolEntrenador) {
            throw new IllegalArgumentException(
                    "El usuario debe tener el rol ENTRENADOR para registrarse como entrenador");
        }

        Entrenador entrenador = Entrenador.builder()
                .persona(persona)
                .usuario(usuario)
                .especialidad(resolverEspecialidad(request.idEspecialidad()))
                .experienciaAnios(request.experienciaAnios())
                .certificacion(request.certificacion())
                .activo(true)
                .build();

        entrenador = entrenadorRepository.save(entrenador);
        return toResponse(entrenador);
    }

    /**
     * Actualiza la especialidad y los datos profesionales de un entrenador.
     *
     * @param id      identificador del entrenador a editar
     * @param request datos nuevos
     * @return el entrenador actualizado
     * @throws ResourceNotFoundException si el entrenador o la especialidad
     *                                      no existen
     */
    @CacheEvict(value = RedisCacheConfig.CACHE_COACHES, allEntries = true)
    @Transactional
    public EntrenadorResponse editar(Long id, EntrenadorRequest request) {
        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado con id: " + id));

        entrenador.setEspecialidad(resolverEspecialidad(request.idEspecialidad()));
        entrenador.setExperienciaAnios(request.experienciaAnios());
        entrenador.setCertificacion(request.certificacion());

        entrenador = entrenadorRepository.save(entrenador);
        return toResponse(entrenador);
    }

    /**
     * Baja lógica de un entrenador ({@code activo = false}).
     *
     * @param id identificador del entrenador
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(accion = "ELIMINAR", entidad = "Entrenador", idSpel = "#p0",
            descripcionSpel = "'desactivo la ficha de entrenador #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_COACHES, allEntries = true)
    @Transactional
    public void eliminar(Long id) {
        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado con id: " + id));
        entrenador.setActivo(false);
        entrenadorRepository.save(entrenador);
    }

    /**
     * Reactiva un entrenador dado de baja.
     *
     * @param id identificador del entrenador
     * @return el entrenador reactivado
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si ya está activo
     */
    @Audited(accion = "REACTIVAR", entidad = "Entrenador", idSpel = "#p0",
            descripcionSpel = "'reactivo la ficha de entrenador #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_COACHES, allEntries = true)
    @Transactional
    public EntrenadorResponse reactivar(Long id) {
        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado con id: " + id));

        if (Boolean.TRUE.equals(entrenador.getActivo())) {
            throw new IllegalArgumentException("La ficha de entrenador ya se encuentra activa");
        }

        entrenador.setActivo(true);
        return toResponse(entrenadorRepository.save(entrenador));
    }

    private Especialidad resolverEspecialidad(Long idEspecialidad) {
        if (idEspecialidad == null) return null;
        return especialidadRepository.findById(idEspecialidad)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + idEspecialidad));
    }

    private EntrenadorResponse toResponse(Entrenador e) {
        return new EntrenadorResponse(
                e.getIdEntrenador(),
                e.getPersona().getIdPersona(),
                e.getPersona().getNombre(),
                e.getPersona().getApellido(),
                e.getPersona().getCedula(),
                e.getPersona().getCorreo(),
                e.getPersona().getTelefono(),
                e.getUsuario().getIdUsuario(),
                e.getUsuario().getUsername(),
                e.getEspecialidad() != null ? e.getEspecialidad().getIdEspecialidad() : null,
                e.getEspecialidad() != null ? e.getEspecialidad().getNombre() : null,
                e.getExperienciaAnios(),
                e.getCertificacion(),
                e.getActivo(),
                e.getCreatedAt()
        );
    }
}
