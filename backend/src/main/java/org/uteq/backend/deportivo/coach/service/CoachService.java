package org.uteq.backend.deportivo.coach.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.deportivo.coach.dto.CoachPageResponse;
import org.uteq.backend.deportivo.coach.dto.CoachRequest;
import org.uteq.backend.deportivo.coach.dto.CoachResponse;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.deportivo.specialty.entity.Specialty;
import org.uteq.backend.deportivo.specialty.repository.SpecialtyRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;

/**
 * Lógica de negocio de {@code Coach}. Cada entrenador se apoya en una
 * {@code Person} y una cuenta de rol {@code ENTRENADOR} ya creadas; la
 * especialidad es opcional. Las bajas son lógicas ({@code activo = false}).
 */
@Service
@RequiredArgsConstructor
public class CoachService {

    private final CoachRepository coachRepository;
    private final PersonRepository personaRepository;
    private final UserAccountRepository usuarioRepository;
    private final SpecialtyRepository specialtyRepository;

    /**
     * Lista paginada de entrenadores.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, envuelta en {@link CoachPageResponse}
     */
    @Cacheable(value = RedisCacheConfig.CACHE_COACHES, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public CoachPageResponse<CoachResponse> list(Pageable pageable) {
        Page<Coach> page = coachRepository.findAll(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new CoachPageResponse<>(
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
    public CoachResponse findById(Long id) {
        Coach e = coachRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coach no encontrado con id: " + id));
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
    public CoachResponse create(CoachRequest request) {
        if (coachRepository.existsByPerson_Id(request.idPersona())) {
            throw new IllegalArgumentException("La persona ya está registrada como entrenador");
        }
        if (coachRepository.existsByUserAccount_Id(request.idUsuario())) {
            throw new IllegalArgumentException("El usuario ya está asignado a otro entrenador");
        }

        Person persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con id: " + request.idPersona()));

        UserAccount usuario = usuarioRepository.findById(request.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + request.idUsuario()));

        boolean tieneRolEntrenador = usuario.getRoles().stream()
                .anyMatch(r -> "ENTRENADOR".equals(r.getName()));
        if (!tieneRolEntrenador) {
            throw new IllegalArgumentException(
                    "El usuario debe tener el rol ENTRENADOR para registrarse como entrenador");
        }

        Coach entrenador = Coach.builder()
                .persona(persona)
                .usuario(usuario)
                .especialidad(resolveSpecialty(request.idEspecialidad()))
                .experienciaAnios(request.experienciaAnios())
                .certificacion(request.certificacion())
                .activo(true)
                .build();

        entrenador = coachRepository.save(entrenador);
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
    public CoachResponse update(Long id, CoachRequest request) {
        Coach entrenador = coachRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coach no encontrado con id: " + id));

        entrenador.setEspecialidad(resolveSpecialty(request.idEspecialidad()));
        entrenador.setExperienciaAnios(request.experienciaAnios());
        entrenador.setCertificacion(request.certificacion());

        entrenador = coachRepository.save(entrenador);
        return toResponse(entrenador);
    }

    /**
     * Baja lógica de un entrenador ({@code activo = false}).
     *
     * @param id identificador del entrenador
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(action = "ELIMINAR", entity = "Coach", idSpel = "#p0",
            descriptionSpel = "'desactivo la ficha de entrenador #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_COACHES, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Coach entrenador = coachRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coach no encontrado con id: " + id));
        entrenador.setActivo(false);
        coachRepository.save(entrenador);
    }

    /**
     * Reactiva un entrenador dado de baja.
     *
     * @param id identificador del entrenador
     * @return el entrenador reactivado
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si ya está activo
     */
    @Audited(action = "REACTIVAR", entity = "Coach", idSpel = "#p0",
            descriptionSpel = "'reactivo la ficha de entrenador #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_COACHES, allEntries = true)
    @Transactional
    public CoachResponse reactivate(Long id) {
        Coach entrenador = coachRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coach no encontrado con id: " + id));

        if (Boolean.TRUE.equals(entrenador.getActivo())) {
            throw new IllegalArgumentException("La ficha de entrenador ya se encuentra activa");
        }

        entrenador.setActivo(true);
        return toResponse(coachRepository.save(entrenador));
    }

    private Specialty resolveSpecialty(Long idEspecialidad) {
        if (idEspecialidad == null) return null;
        return specialtyRepository.findById(idEspecialidad)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty no encontrada con id: " + idEspecialidad));
    }

    private CoachResponse toResponse(Coach e) {
        return new CoachResponse(
                e.getIdEntrenador(),
                e.getPersona().getId(),
                e.getPersona().getName(),
                e.getPersona().getLastName(),
                e.getPersona().getNationalId(),
                e.getPersona().getEmail(),
                e.getPersona().getPhone(),
                e.getUsuario().getId(),
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
