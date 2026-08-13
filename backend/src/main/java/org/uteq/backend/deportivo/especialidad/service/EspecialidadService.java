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

@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    @Transactional(readOnly = true)
    public Page<EspecialidadResponse> listarPaginado(Pageable pageable) {
        return especialidadRepository.findByActivoTrue(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listarTodasActivas() {
        return especialidadRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EspecialidadResponse buscarPorId(Long id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada con ID: " + id));
        return toResponse(especialidad);
    }

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
