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

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final PersonaRepository personaRepository;

    /**
     * Listado paginado con cache Redis (Bloque A.1). TTL declarado en
     * configuración externa. La clave incluye página, tamaño y orden para
     * cachear cada combinación por separado.
     */
    @Cacheable(value = RedisCacheConfig.CACHE_ESTUDIANTES,
            key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public PageResponse<EstudianteResponse> listar(Pageable pageable) {
        Page<EstudianteResponse> page = estudianteRepository
                .findByActivoTrue(pageable)
                .map(this::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public EstudianteResponse buscarPorId(Long id) {
        return estudianteRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado: " + id));
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        Persona persona = Persona.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .activo(true)
                .build();
        personaRepository.save(persona);

        Estudiante estudiante = Estudiante.builder()
                .persona(persona)
                .categoria(request.categoria())
                .build();
        estudianteRepository.save(estudiante);

        return toResponse(estudiante);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse actualizar(Long id, EstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado: " + id));

        estudiante.getPersona().setNombre(request.nombre());
        estudiante.getPersona().setApellido(request.apellido());
        estudiante.setCategoria(request.categoria());
        estudianteRepository.save(estudiante);

        return toResponse(estudiante);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public void eliminar(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado: " + id));
        estudiante.setActivo(false);
        estudianteRepository.save(estudiante);
    }

    /**
     * Reporte agregado por categoría. Es una operación con COUNT/GROUP BY,
     * por lo que el Bloque A.2.2 obliga a resolverla en el motor mediante
     * función SQL, no en JPQL. Se invoca vía @Procedure (JPA 2.1).
     */
    @Transactional(readOnly = true)
    public long contarActivosPorCategoria(String categoria) {
        return estudianteRepository.contarActivosPorCategoria(categoria);
    }

    /**
     * Baja lógica masiva por categoría: actualización sobre múltiples filas
     * con criterio de negocio -> procedimiento almacenado (Bloque A.2.2).
     */
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public int desactivarPorCategoria(String categoria) {
        return estudianteRepository.desactivarPorCategoria(categoria);
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
