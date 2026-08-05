package org.uteq.backend.academico.estudianteRepresentante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.estudianteRepresentante.dto.EstudianteRepresentanteRequest;
import org.uteq.backend.academico.estudianteRepresentante.dto.EstudianteRepresentanteResponse;
import org.uteq.backend.academico.estudianteRepresentante.entity.EstudianteRepresentante;
import org.uteq.backend.academico.estudianteRepresentante.repository.EstudianteRepresentanteRepository;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstudianteRepresentanteService {

    private final EstudianteRepresentanteRepository estudianteRepresentanteRepository;
    private final EstudianteRepository estudianteRepository;
    private final RepresentanteRepository representanteRepository;

    @Transactional(readOnly = true)
    public Page<EstudianteRepresentanteResponse> listar(Pageable pageable) {
        return estudianteRepresentanteRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EstudianteRepresentanteResponse> listarPorEstudiante(Long idEstudiante) {
        return estudianteRepresentanteRepository.findByEstudiante_IdEstudiante(idEstudiante).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstudianteRepresentanteResponse buscarPorId(Long id) {
        EstudianteRepresentante er = estudianteRepresentanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Relación estudiante-representante no encontrada con id: " + id));
        return toResponse(er);
    }

    @Transactional
    public EstudianteRepresentanteResponse crear(EstudianteRepresentanteRequest request) {
        if (estudianteRepresentanteRepository.existsByEstudiante_IdEstudianteAndRepresentante_IdRepresentante(
                request.idEstudiante(), request.idRepresentante())) {
            throw new IllegalArgumentException("Ese representante ya está vinculado a este estudiante");
        }

        Estudiante estudiante = estudianteRepository.findById(request.idEstudiante())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + request.idEstudiante()));

        Representante representante = representanteRepository.findById(request.idRepresentante())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Representante no encontrado con id: " + request.idRepresentante()));

        EstudianteRepresentante er = EstudianteRepresentante.builder()
                .estudiante(estudiante)
                .representante(representante)
                .relacion(request.relacion())
                .contactoPrincipal(request.contactoPrincipal() != null ? request.contactoPrincipal() : false)
                .build();

        er = estudianteRepresentanteRepository.save(er);
        return toResponse(er);
    }

    @Transactional
    public EstudianteRepresentanteResponse editar(Long id, EstudianteRepresentanteRequest request) {
        EstudianteRepresentante er = estudianteRepresentanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Relación estudiante-representante no encontrada con id: " + id));

        boolean cambiaEstudiante = !er.getEstudiante().getIdEstudiante().equals(request.idEstudiante());
        boolean cambiaRepresentante = !er.getRepresentante().getIdRepresentante().equals(request.idRepresentante());

        if ((cambiaEstudiante || cambiaRepresentante)
                && estudianteRepresentanteRepository.existsByEstudiante_IdEstudianteAndRepresentante_IdRepresentante(
                        request.idEstudiante(), request.idRepresentante())) {
            throw new IllegalArgumentException("Ese representante ya está vinculado a este estudiante");
        }

        if (cambiaEstudiante) {
            Estudiante estudiante = estudianteRepository.findById(request.idEstudiante())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Estudiante no encontrado con id: " + request.idEstudiante()));
            er.setEstudiante(estudiante);
        }

        if (cambiaRepresentante) {
            Representante representante = representanteRepository.findById(request.idRepresentante())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Representante no encontrado con id: " + request.idRepresentante()));
            er.setRepresentante(representante);
        }

        er.setRelacion(request.relacion());
        er.setContactoPrincipal(request.contactoPrincipal() != null ? request.contactoPrincipal() : false);

        er = estudianteRepresentanteRepository.save(er);
        return toResponse(er);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!estudianteRepresentanteRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Relación estudiante-representante no encontrada con id: " + id);
        }
        estudianteRepresentanteRepository.deleteById(id);
    }

    private EstudianteRepresentanteResponse toResponse(EstudianteRepresentante er) {
        return new EstudianteRepresentanteResponse(
                er.getIdEstudianteRepresentante(),
                er.getEstudiante().getIdEstudiante(),
                er.getEstudiante().getCodigoEstudiante(),
                er.getEstudiante().getPersona().getNombre(),
                er.getEstudiante().getPersona().getApellido(),
                er.getRepresentante().getIdRepresentante(),
                er.getRepresentante().getPersona().getNombre(),
                er.getRepresentante().getPersona().getApellido(),
                er.getRepresentante().getPersona().getTelefono(),
                er.getRelacion(),
                er.getContactoPrincipal()
        );
    }
}
