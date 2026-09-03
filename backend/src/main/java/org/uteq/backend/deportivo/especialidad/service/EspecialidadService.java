package org.uteq.backend.deportivo.especialidad.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadRequest;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadResponse;
import org.uteq.backend.deportivo.especialidad.entity.Especialidad;
import org.uteq.backend.deportivo.especialidad.repository.EspecialidadRepository;

import java.util.List;

/**
 * Lógica de negocio del catálogo de especialidades de entrenador. Las bajas
 * son lógicas ({@code activo = false}); la unicidad del nombre se valida en
 * el servicio, sin distinción de mayúsculas.
 */
@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    /**
     * Lista paginada de especialidades activas.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, mapeada a {@link EspecialidadResponse}
     */
    @Transactional(readOnly = true)
    public Page<EspecialidadResponse> listarPaginado(Pageable pageable) {
        return especialidadRepository.findByActivoTrue(pageable).map(this::toResponse);
    }

    /**
     * Lista completa de especialidades activas, sin paginar.
     *
     * @return todas las especialidades activas
     */
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listarTodasActivas() {
        return especialidadRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca una especialidad por su identificador.
     *
     * @param id identificador de la especialidad
     * @return la especialidad encontrada
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public EspecialidadResponse buscarPorId(Long id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada con ID: " + id));
        return toResponse(especialidad);
    }

    /**
     * Crea una especialidad.
     *
     * @param request nombre de la especialidad
     * @return la especialidad creada
     * @throws IllegalArgumentException si ya existe una con ese nombre
     */
    @Transactional
    public EspecialidadResponse crear(EspecialidadRequest request) {
        if (especialidadRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre");
        }

        Especialidad especialidad = Especialidad.builder()
                .nombre(request.nombre())
                .activo(true)
                .build();

        return toResponse(especialidadRepository.save(especialidad));
    }

    /**
     * Actualiza el nombre de una especialidad.
     *
     * @param id      identificador de la especialidad a editar
     * @param request datos nuevos
     * @return la especialidad actualizada
     * @throws RecursoNoEncontradoException si no existe
     * @throws IllegalArgumentException     si el nombre nuevo pertenece a
     *                                      otra especialidad
     */
    @Transactional
    public EspecialidadResponse editar(Long id, EspecialidadRequest request) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada con ID: " + id));

        if (!especialidad.getNombre().equalsIgnoreCase(request.nombre())
                && especialidadRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre");
        }

        especialidad.setNombre(request.nombre());

        return toResponse(especialidadRepository.save(especialidad));
    }

    /**
     * Baja lógica de una especialidad ({@code activo = false}).
     *
     * @param id identificador de la especialidad
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional
    public void eliminar(Long id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada con ID: " + id));
        especialidad.setActivo(false);
        especialidadRepository.save(especialidad);
    }

    private EspecialidadResponse toResponse(Especialidad especialidad) {
        return new EspecialidadResponse(
                especialidad.getIdEspecialidad(),
                especialidad.getNombre(),
                especialidad.getActivo(),
                especialidad.getCreatedAt()
        );
    }
}
