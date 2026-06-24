package org.uteq.backend.estudiante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.uteq.backend.auth.entity.Persona;
import org.uteq.backend.auth.repository.PersonaRepository;
import org.uteq.backend.estudiante.dto.EstudianteRequest;
import org.uteq.backend.estudiante.dto.EstudianteResponse;
import org.uteq.backend.estudiante.entity.Estudiante;
import org.uteq.backend.estudiante.repository.EstudianteRepository;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final PersonaRepository personaRepository;

    public Page<EstudianteResponse> listar(Pageable pageable) {
        return estudianteRepository.findByActivoTrue(pageable)
                .map(this::toResponse);
    }

    public EstudianteResponse buscarPorId(Long id) {
        return estudianteRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
    }

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

    public EstudianteResponse actualizar(Long id, EstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        estudiante.getPersona().setNombre(request.nombre());
        estudiante.getPersona().setApellido(request.apellido());
        estudiante.setCategoria(request.categoria());
        estudianteRepository.save(estudiante);

        return toResponse(estudiante);
    }

    public void eliminar(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        estudiante.setActivo(false);
        estudianteRepository.save(estudiante);
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