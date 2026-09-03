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
import org.uteq.backend.academico.representante.dto.VinculoRequest;
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
import org.uteq.backend.seguridad.auditoria.aop.Auditado;

/**
 * CRUD administrativo de {@code Representante}. El alta y la vinculación con
 * estudiantes son operaciones de {@code ADMINISTRADOR} / {@code RECEPCIONISTA}:
 * un representante no se autoregistra, lo da de alta un administrador ya
 * vinculándolo a sus representados. Un estudiante tiene un solo contacto
 * principal: designar uno nuevo desplaza al anterior.
 */
@Service
@RequiredArgsConstructor
public class RepresentanteService {
    private final RepresentanteRepository representanteRepository;
    private final RepresentanteEstudianteRepository vinculoRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;

    /**
     * Lista paginada de representantes.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, envuelta en {@link RepresentantePageResponse}
     */
    @Transactional(readOnly = true)
    public RepresentantePageResponse<RepresentanteResponse> listar(Pageable pageable) {
        Page<Representante> page = representanteRepository.findAll(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new RepresentantePageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Busca un representante por su identificador.
     *
     * @param id identificador del representante
     * @return el representante encontrado
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public RepresentanteResponse buscarPorId(Long id) {
        Representante r = representanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + id));
        return toResponse(r);
    }

    /**
     * Registra un representante sobre una persona y una cuenta ya existentes,
     * y opcionalmente lo vincula a una lista inicial de estudiantes.
     *
     * @param request persona, usuario, parentesco, contacto y representados
     *                iniciales
     * @return el representante registrado
     * @throws RecursoNoEncontradoException si la persona, el usuario o algún
     *                                      estudiante inicial no existen
     * @throws IllegalArgumentException     si la persona o el usuario ya
     *                                      están asignados, o si el usuario
     *                                      no tiene rol {@code REPRESENTANTE}
     */
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

        boolean tieneRolRepresentante = usuario.getRoles().stream()
                .anyMatch(r -> "REPRESENTANTE".equals(r.getNombre()));
        if (!tieneRolRepresentante) {
            throw new IllegalArgumentException(
                    "El usuario debe tener el rol REPRESENTANTE para registrarse como representante");
        }

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

    /**
     * Actualiza el parentesco y el teléfono de contacto de un representante.
     *
     * @param id      identificador del representante
     * @param request datos nuevos
     * @return el representante actualizado
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional
    public RepresentanteResponse editar(Long id, RepresentanteRequest request) {
        Representante representante = representanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + id));
        representante.setParentesco(request.parentesco());
        representante.setTelefonoContacto(request.telefonoContacto());
        representante = representanteRepository.save(representante);
        return toResponse(representante);
    }

    /**
     * Baja lógica de un representante ({@code activo = false}).
     *
     * @param id identificador del representante
     * @throws RecursoNoEncontradoException si no existe
     */
    @Auditado(accion = "ELIMINAR", entidad = "Representante", idSpel = "#p0",
            descripcionSpel = "'desactivo la ficha de representante #' + #p0")
    @Transactional
    public void eliminar(Long id) {
        Representante representante = representanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + id));
        representante.setActivo(false);
        representanteRepository.save(representante);
    }

    /**
     * Reactiva un representante dado de baja.
     *
     * @param id identificador del representante
     * @return el representante reactivado
     * @throws RecursoNoEncontradoException si no existe
     * @throws IllegalArgumentException     si ya está activo
     */
    @Auditado(accion = "REACTIVAR", entidad = "Representante", idSpel = "#p0",
            descripcionSpel = "'reactivo la ficha de representante #' + #p0")
    @Transactional
    public RepresentanteResponse reactivar(Long id) {
        Representante representante = representanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + id));

        if (Boolean.TRUE.equals(representante.getActivo())) {
            throw new IllegalArgumentException("La ficha de representante ya se encuentra activa");
        }

        representante.setActivo(true);
        return toResponse(representanteRepository.save(representante));
    }

    /**
     * Vincula un estudiante a un representante (o reactiva y actualiza el
     * vínculo si ya existía).
     *
     * @param idRepresentante identificador del representante
     * @param idEstudiante    identificador del estudiante
     * @param request         relación y marca de contacto principal; puede
     *                        ser {@code null}
     * @return el representante con su lista de representados actualizada
     * @throws RecursoNoEncontradoException si el representante o el estudiante
     *                                      no existen
     */
    @Transactional
    public RepresentanteResponse vincularEstudiante(Long idRepresentante, Long idEstudiante, VinculoRequest request) {
        Representante representante = representanteRepository.findById(idRepresentante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Representante no encontrado con id: " + idRepresentante));
        String relacion = request == null ? null : request.relacion();
        boolean contactoPrincipal = request != null && Boolean.TRUE.equals(request.contactoPrincipal());
        vincular(representante, idEstudiante, relacion, contactoPrincipal);
        return toResponse(representante);
    }

    /**
     * Desvincula un estudiante de un representante: baja lógica del vínculo
     * puntual, sin tocar la cuenta ni los demás representados.
     *
     * @param idRepresentante identificador del representante
     * @param idEstudiante    identificador del estudiante
     * @throws RecursoNoEncontradoException si no hay un vínculo entre ambos
     */
    @Transactional
    public void desvincularEstudiante(Long idRepresentante, Long idEstudiante) {
        RepresentanteEstudiante vinculo = vinculoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(idRepresentante, idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ese estudiante no está vinculado a este representante"));
        vinculo.setActivo(false);
        vinculoRepository.save(vinculo);
    }

    private void vincular(Representante representante, Long idEstudiante) {
        vincular(representante, idEstudiante, null, false);
    }

    // Un estudiante tiene un solo contacto principal: designar uno nuevo
    // desplaza al anterior en vez de dejar dos marcados.
    private void vincular(Representante representante, Long idEstudiante,
                          String relacion, boolean contactoPrincipal) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante));

        if (contactoPrincipal) {
            vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(idEstudiante).stream()
                    .filter(v -> !v.getRepresentante().getIdRepresentante().equals(representante.getIdRepresentante()))
                    .filter(v -> Boolean.TRUE.equals(v.getContactoPrincipal()))
                    .forEach(v -> {
                        v.setContactoPrincipal(false);
                        vinculoRepository.save(v);
                    });
        }

        vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(
                        representante.getIdRepresentante(), idEstudiante)
                .ifPresentOrElse(
                        existente -> {
                            existente.setActivo(true);
                            existente.setRelacion(relacion);
                            existente.setContactoPrincipal(contactoPrincipal);
                            vinculoRepository.save(existente);
                        },
                        () -> vinculoRepository.save(RepresentanteEstudiante.builder()
                                .representante(representante)
                                .estudiante(estudiante)
                                .activo(true)
                                .relacion(relacion)
                                .contactoPrincipal(contactoPrincipal)
                                .build())
                );
    }

    private RepresentanteResponse toResponse(Representante r) {
        List<EstudianteVinculadoResponse> representados =
                vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(r.getIdRepresentante()).stream()
                        .map(v -> new EstudianteVinculadoResponse(
                                v.getEstudiante().getIdEstudiante(),
                                v.getEstudiante().getPersona().getNombre() + " " + v.getEstudiante().getPersona().getApellido(),
                                v.getEstudiante().getCategoria().getNombre(),
                                v.getRelacion(),
                                v.getContactoPrincipal()))
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
