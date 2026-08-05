package org.uteq.backend.academico.representante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.representante.dto.RepresentanteRequest;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;

@Service
@RequiredArgsConstructor
public class RepresentanteService {

    private final RepresentanteRepository representanteRepository;
    private final PersonaRepository personaRepository;

    @Transactional(readOnly = true)
    public Page<RepresentanteResponse> listar(Pageable pageable) {
        return representanteRepository.findByActivoTrue(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RepresentanteResponse buscarPorId(Long id) {
        Representante r = representanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + id));
        return toResponse(r);
    }

    @Transactional
    public RepresentanteResponse crear(RepresentanteRequest request) {
        if (representanteRepository.existsByPersona_IdPersona(request.idPersona())) {
            throw new IllegalArgumentException("La persona ya está registrada como representante");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con id: " + request.idPersona()));

        Representante representante = Representante.builder()
                .persona(persona)
                .ocupacion(request.ocupacion())
                .activo(true)
                .build();

        representante = representanteRepository.save(representante);
        return toResponse(representante);
    }

    @Transactional
    public RepresentanteResponse editar(Long id, RepresentanteRequest request) {
        Representante representante = representanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + id));

        if (!representante.getPersona().getIdPersona().equals(request.idPersona())) {
            if (representanteRepository.existsByPersona_IdPersona(request.idPersona())) {
                throw new IllegalArgumentException("La nueva persona seleccionada ya es un representante registrado");
            }
            Persona nuevaPersona = personaRepository.findById(request.idPersona())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con id: " + request.idPersona()));
            representante.setPersona(nuevaPersona);
        }

        representante.setOcupacion(request.ocupacion());

        representante = representanteRepository.save(representante);
        return toResponse(representante);
    }

    @Transactional
    public void eliminar(Long id) {
        Representante representante = representanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + id));
        representante.setActivo(false);
        representanteRepository.save(representante);
    }

    private RepresentanteResponse toResponse(Representante r) {
        return new RepresentanteResponse(
                r.getIdRepresentante(),
                r.getPersona().getIdPersona(),
                r.getPersona().getNombre(),
                r.getPersona().getApellido(),
                r.getPersona().getCedula(),
                r.getPersona().getTelefono(),
                r.getOcupacion(),
                r.getActivo()
        );
    }
}
