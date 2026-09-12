package org.uteq.backend.deportivo.specialty.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.specialty.dto.SpecialtyRequest;
import org.uteq.backend.deportivo.specialty.dto.SpecialtyResponse;
import org.uteq.backend.deportivo.specialty.entity.Specialty;
import org.uteq.backend.deportivo.specialty.repository.SpecialtyRepository;

import java.util.List;

/**
 * Lógica de negocio del catálogo de especialidades de entrenador. Las bajas
 * son lógicas ({@code activo = false}); la unicidad del nombre se valida en
 * el servicio, sin distinción de mayúsculas.
 */
@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    /**
     * Lista paginada de especialidades activas.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, mapeada a {@link SpecialtyResponse}
     */
    @Transactional(readOnly = true)
    public Page<SpecialtyResponse> findPaged(Pageable pageable) {
        return specialtyRepository.findActiveTrue(pageable).map(this::toResponse);
    }

    /**
     * Lista completa de especialidades activas, sin paginar.
     *
     * @return todas las especialidades activas
     */
    @Transactional(readOnly = true)
    public List<SpecialtyResponse> findAllActive() {
        return specialtyRepository.findActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca una especialidad por su identificador.
     *
     * @param id identificador de la especialidad
     * @return la especialidad encontrada
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public SpecialtyResponse findById(Long id) {
        Specialty especialidad = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty no encontrada con ID: " + id));
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
    public SpecialtyResponse create(SpecialtyRequest request) {
        if (specialtyRepository.existsByNameIgnoreCase(request.nombre())) {
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre");
        }

        Specialty especialidad = Specialty.builder()
                .nombre(request.nombre())
                .activo(true)
                .build();

        return toResponse(specialtyRepository.save(especialidad));
    }

    /**
     * Actualiza el nombre de una especialidad.
     *
     * @param id      identificador de la especialidad a editar
     * @param request datos nuevos
     * @return la especialidad actualizada
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si el nombre nuevo pertenece a
     *                                      otra especialidad
     */
    @Transactional
    public SpecialtyResponse update(Long id, SpecialtyRequest request) {
        Specialty especialidad = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty no encontrada con ID: " + id));

        if (!especialidad.getNombre().equalsIgnoreCase(request.nombre())
                && specialtyRepository.existsByNameIgnoreCase(request.nombre())) {
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre");
        }

        especialidad.setNombre(request.nombre());

        return toResponse(specialtyRepository.save(especialidad));
    }

    /**
     * Baja lógica de una especialidad ({@code activo = false}).
     *
     * @param id identificador de la especialidad
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional
    public void delete(Long id) {
        Specialty especialidad = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty no encontrada con ID: " + id));
        especialidad.setActivo(false);
        specialtyRepository.save(especialidad);
    }

    private SpecialtyResponse toResponse(Specialty especialidad) {
        return new SpecialtyResponse(
                especialidad.getIdEspecialidad(),
                especialidad.getNombre(),
                especialidad.getActivo(),
                especialidad.getCreatedAt()
        );
    }
}
