package org.uteq.backend.estudiante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.auth.entity.Persona;
import org.uteq.backend.auth.repository.PersonaRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.estudiante.dto.EstudianteRequest;
import org.uteq.backend.estudiante.dto.EstudianteResponse;
import org.uteq.backend.estudiante.dto.PageResponse;
import org.uteq.backend.estudiante.entity.Estudiante;
import org.uteq.backend.estudiante.repository.EstudianteRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final PersonaRepository personaRepository;

    @Cacheable(value = RedisCacheConfig.CACHE_ESTUDIANTES, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PageResponse<EstudianteResponse> listar(Pageable pageable) {
        Page<Estudiante> page = estudianteRepository.findByActivoTrue(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public EstudianteResponse buscarPorId(Long id) {
        Estudiante e = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));
        return toResponse(e);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        Persona persona = Persona.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .activo(true)
                .build();
        persona = personaRepository.save(persona);

        Estudiante estudiante = Estudiante.builder()
                .persona(persona)
                .categoria(request.categoria())
                .fechaIngreso(Instant.now())
                .activo(true)
                .build();
        estudiante = estudianteRepository.save(estudiante);

        return toResponse(estudiante);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse editar(Long id, EstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));

        Persona persona = estudiante.getPersona();
        persona.setNombre(request.nombre());
        persona.setApellido(request.apellido());
        personaRepository.save(persona);

        estudiante.setCategoria(request.categoria());
        estudiante = estudianteRepository.save(estudiante);

        return toResponse(estudiante);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public void eliminar(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));
        estudiante.setActivo(false);
        estudianteRepository.save(estudiante);
    }

    @Transactional(readOnly = true)
    public long contarActivosPorCategoria(String categoria) {
        return estudianteRepository.contarActivosPorCategoria(categoria);
    }

    private EstudianteResponse toResponse(Estudiante e) {
        return new EstudianteResponse(
                e.getIdEstudiante(),
                e.getPersona().getNombre(),
                e.getPersona().getApellido(),
                e.getCategoria(),
                e.getActivo(),
                e.getCreadoEn()
        );
    }
}
