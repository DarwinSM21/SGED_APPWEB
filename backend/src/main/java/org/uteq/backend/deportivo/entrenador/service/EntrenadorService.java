package org.uteq.backend.deportivo.entrenador.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorPageResponse;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorRequest;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorResponse;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.especialidad.entity.Especialidad;
import org.uteq.backend.deportivo.especialidad.repository.EspecialidadRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;

@Service
@RequiredArgsConstructor
public class EntrenadorService {

    private final EntrenadorRepository entrenadorRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;

    @Cacheable(value = RedisCacheConfig.CACHE_ENTRENADORES, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
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

    @Transactional(readOnly = true)
    public EntrenadorResponse buscarPorId(Long id) {
        Entrenador e = entrenadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador no encontrado con id: " + id));
        return toResponse(e);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ENTRENADORES, allEntries = true)
    @Transactional
    public EntrenadorResponse crear(EntrenadorRequest request) {
        if (entrenadorRepository.existsByPersona_IdPersona(request.idPersona())) {
            throw new IllegalArgumentException("La persona ya está registrada como entrenador");
        }
        if (entrenadorRepository.existsByUsuario_IdUsuario(request.idUsuario())) {
            throw new IllegalArgumentException("El usuario ya está asignado a otro entrenador");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con id: " + request.idPersona()));

        Usuario usuario = usuarioRepository.findById(request.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + request.idUsuario()));

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

    @CacheEvict(value = RedisCacheConfig.CACHE_ENTRENADORES, allEntries = true)
    @Transactional
    public EntrenadorResponse editar(Long id, EntrenadorRequest request) {
        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador no encontrado con id: " + id));

        entrenador.setEspecialidad(resolverEspecialidad(request.idEspecialidad()));
        entrenador.setExperienciaAnios(request.experienciaAnios());
        entrenador.setCertificacion(request.certificacion());

        entrenador = entrenadorRepository.save(entrenador);
        return toResponse(entrenador);
    }

    @Auditado(accion = "ELIMINAR", entidad = "Entrenador", idSpel = "#p0",
            descripcionSpel = "'desactivo la ficha de entrenador #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_ENTRENADORES, allEntries = true)
    @Transactional
    public void eliminar(Long id) {
        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador no encontrado con id: " + id));
        entrenador.setActivo(false);
        entrenadorRepository.save(entrenador);
    }

    @Auditado(accion = "REACTIVAR", entidad = "Entrenador", idSpel = "#p0",
            descripcionSpel = "'reactivo la ficha de entrenador #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_ENTRENADORES, allEntries = true)
    @Transactional
    public EntrenadorResponse reactivar(Long id) {
        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador no encontrado con id: " + id));

        if (Boolean.TRUE.equals(entrenador.getActivo())) {
            throw new IllegalArgumentException("La ficha de entrenador ya se encuentra activa");
        }

        entrenador.setActivo(true);
        return toResponse(entrenadorRepository.save(entrenador));
    }

    private Especialidad resolverEspecialidad(Long idEspecialidad) {
        if (idEspecialidad == null) return null;
        return especialidadRepository.findById(idEspecialidad)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada con id: " + idEspecialidad));
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