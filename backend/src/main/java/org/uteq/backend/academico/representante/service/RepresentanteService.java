package org.uteq.backend.academico.representante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.dto.RepresentantePageResponse;
import org.uteq.backend.academico.representante.dto.RepresentanteRequest;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse.EstudianteVinculadoResponse;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.entity.RepresentanteEstudiante;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.List;

/**
 * CRUD administrativo de Representante. El alta y la vinculacion con
 * estudiantes son operaciones de ADMINISTRADOR (mismo criterio que
 * Entrenador): un representante no se autoregistra, lo da de alta un
 * administrador ya vinculandolo a sus representados.
 */
@Service
@RequiredArgsConstructor
public class RepresentanteService {

    private final RepresentanteRepository representanteRepository;
    private final RepresentanteEstudianteRepository vinculoRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;

    @Transactional(readOnly = true)
    public RepresentantePageResponse<RepresentanteResponse> listar(Pageable pageable) {
        Page<Representante> page = representanteRepository.findByActivoTrue(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new RepresentantePageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
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
        if (representanteRepository.existsByUsuario_IdUsuario(request.idUsuario())) {
            throw new IllegalArgumentException("El usuario ya está asignado a otro representante");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con id: " + request.idPersona()));
        Usuario usuario = usuarioRepository.findById(request.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + request.idUsuario()));

        Representante representante = Representante.builder()
                .persona(persona)
                .usuario(usuario)
                .parentesco(request.parentesco())
                .telefonoContacto(request.telefonoContacto())
                .activo(true)
                .build();
        representante = representanteRepository.save(representante);

        List<Long> idsIniciales = request.idsEstudiantesIniciales();
        if (idsIniciales != null) {
            for (Long idEstudiante : idsIniciales) {
                vincular(representante, idEstudiante);
            }
        }

        return toResponse(representante);
    }

    @Transactional
    public RepresentanteResponse editar(Long id, RepresentanteRequest request) {
        Representante representante = representanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + id));
        representante.setParentesco(request.parentesco());
        representante.setTelefonoContacto(request.telefonoContacto());
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

    @Transactional
    public RepresentanteResponse vincularEstudiante(Long idRepresentante, Long idEstudiante) {
        Representante representante = representanteRepository.findById(idRepresentante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + idRepresentante));
        vincular(representante, idEstudiante);
        return toResponse(representante);
    }

    /** Baja logica del vinculo puntual: no toca la cuenta ni los demas representados. */
    @Transactional
    public void desvincularEstudiante(Long idRepresentante, Long idEstudiante) {
        RepresentanteEstudiante vinculo = vinculoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(idRepresentante, idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ese estudiante no está vinculado a este representante"));
        vinculo.setActivo(false);
        vinculoRepository.save(vinculo);
    }

    private void vincular(Representante representante, Long idEstudiante) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante));

        vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(
                        representante.getIdRepresentante(), idEstudiante)
                .ifPresentOrElse(
                        existente -> {
                            existente.setActivo(true);
                            vinculoRepository.save(existente);
                        },
                        () -> vinculoRepository.save(RepresentanteEstudiante.builder()
                                .representante(representante)
                                .estudiante(estudiante)
                                .activo(true)
                                .build())
                );
    }

    private RepresentanteResponse toResponse(Representante r) {
        List<EstudianteVinculadoResponse> representados =
                vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(r.getIdRepresentante()).stream()
                        .map(v -> new EstudianteVinculadoResponse(
                                v.getEstudiante().getIdEstudiante(),
                                v.getEstudiante().getPersona().getNombre() + " " + v.getEstudiante().getPersona().getApellido(),
                                v.getEstudiante().getCategoria().getNombre()))
                        .toList();

        return new RepresentanteResponse(
                r.getIdRepresentante(),
                r.getPersona().getIdPersona(),
                r.getPersona().getNombre(),
                r.getPersona().getApellido(),
                r.getPersona().getCedula(),
                r.getPersona().getCorreo(),
                r.getUsuario().getIdUsuario(),
                r.getUsuario().getUsername(),
                r.getParentesco(),
                r.getTelefonoContacto(),
                r.getActivo(),
                r.getCreatedAt(),
                representados
        );
    }
}
