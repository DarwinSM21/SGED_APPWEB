package org.uteq.backend.academico.guardian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.guardian.dto.GuardianPageResponse;
import org.uteq.backend.academico.guardian.dto.GuardianRequest;
import org.uteq.backend.academico.guardian.dto.GuardianResponse;
import org.uteq.backend.academico.guardian.dto.GuardianResponse.LinkedStudentResponse;
import org.uteq.backend.academico.guardian.dto.LinkRequest;
import org.uteq.backend.academico.guardian.entity.Guardian;
import org.uteq.backend.academico.guardian.entity.GuardianStudent;
import org.uteq.backend.academico.guardian.repository.GuardianStudentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.util.List;
import org.uteq.backend.seguridad.audit.aop.Audited;

/**
 * CRUD administrativo de {@code Guardian}. El alta y la vinculación con
 * estudiantes son operaciones de {@code ADMINISTRADOR} / {@code RECEPCIONISTA}:
 * un representante no se autoregistra, lo da de alta un administrador ya
 * vinculándolo a sus representados. Un estudiante tiene un solo contacto
 * principal: designar uno nuevo desplaza al anterior.
 */
@Service
@RequiredArgsConstructor
public class GuardianService {
    private final GuardianRepository representanteRepository;
    private final GuardianStudentRepository vinculoRepository;
    private final PersonRepository personaRepository;
    private final UserAccountRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;

    /**
     * Lista paginada de representantes.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, envuelta en {@link GuardianPageResponse}
     */
    @Transactional(readOnly = true)
    public GuardianPageResponse<GuardianResponse> list(Pageable pageable) {
        Page<Guardian> page = representanteRepository.findAll(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new GuardianPageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Busca un representante por su identificador.
     *
     * @param id identificador del representante
     * @return el representante encontrado
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public GuardianResponse findById(Long id) {
        Guardian r = representanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Representante no encontrado con id: " + id));
        return toResponse(r);
    }

    /**
     * Registra un representante sobre una persona y una cuenta ya existentes,
     * y opcionalmente lo vincula a una lista inicial de estudiantes.
     *
     * @param request persona, usuario, parentesco, contacto y representados
     *                iniciales
     * @return el representante registrado
     * @throws ResourceNotFoundException si la persona, el usuario o algún
     *                                      estudiante inicial no existen
     * @throws IllegalArgumentException     si la persona o el usuario ya
     *                                      están asignados, o si el usuario
     *                                      no tiene rol {@code REPRESENTANTE}
     */
    @Transactional
    public GuardianResponse create(GuardianRequest request) {
        if (representanteRepository.existsByPersona_IdPersona(request.idPersona())) {
            throw new IllegalArgumentException("La persona ya está registrada como representante");
        }
        if (representanteRepository.existsByUsuario_IdUsuario(request.idUsuario())) {
            throw new IllegalArgumentException("El usuario ya está asignado a otro representante");
        }

        Person persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con id: " + request.idPersona()));
        UserAccount usuario = usuarioRepository.findById(request.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + request.idUsuario()));

        boolean tieneRolRepresentante = usuario.getRoles().stream()
                .anyMatch(r -> "REPRESENTANTE".equals(r.getNombre()));
        if (!tieneRolRepresentante) {
            throw new IllegalArgumentException(
                    "El usuario debe tener el rol REPRESENTANTE para registrarse como representante");
        }

        Guardian representante = Guardian.builder()
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
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional
    public GuardianResponse update(Long id, GuardianRequest request) {
        Guardian representante = representanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Representante no encontrado con id: " + id));
        representante.setParentesco(request.parentesco());
        representante.setTelefonoContacto(request.telefonoContacto());
        representante = representanteRepository.save(representante);
        return toResponse(representante);
    }

    /**
     * Baja lógica de un representante ({@code activo = false}).
     *
     * @param id identificador del representante
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(accion = "ELIMINAR", entidad = "Representante", idSpel = "#p0",
            descripcionSpel = "'desactivo la ficha de representante #' + #p0")
    @Transactional
    public void delete(Long id) {
        Guardian representante = representanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Representante no encontrado con id: " + id));
        representante.setActivo(false);
        representanteRepository.save(representante);
    }

    /**
     * Reactiva un representante dado de baja.
     *
     * @param id identificador del representante
     * @return el representante reactivado
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si ya está activo
     */
    @Audited(accion = "REACTIVAR", entidad = "Representante", idSpel = "#p0",
            descripcionSpel = "'reactivo la ficha de representante #' + #p0")
    @Transactional
    public GuardianResponse reactivate(Long id) {
        Guardian representante = representanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Representante no encontrado con id: " + id));

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
     * @throws ResourceNotFoundException si el representante o el estudiante
     *                                      no existen
     */
    @Transactional
    public GuardianResponse linkStudent(Long idRepresentante, Long idEstudiante, LinkRequest request) {
        Guardian representante = representanteRepository.findById(idRepresentante)
                .orElseThrow(() -> new ResourceNotFoundException("Representante no encontrado con id: " + idRepresentante));
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
     * @throws ResourceNotFoundException si no hay un vínculo entre ambos
     */
    @Transactional
    public void unlinkStudent(Long idRepresentante, Long idEstudiante) {
        GuardianStudent vinculo = vinculoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(idRepresentante, idEstudiante)
                .orElseThrow(() -> new ResourceNotFoundException("Ese estudiante no está vinculado a este representante"));
        vinculo.setActivo(false);
        vinculoRepository.save(vinculo);
    }

    private void vincular(Guardian representante, Long idEstudiante) {
        vincular(representante, idEstudiante, null, false);
    }

    // Un estudiante tiene un solo contacto principal: designar uno nuevo
    // desplaza al anterior en vez de dejar dos marcados.
    private void vincular(Guardian representante, Long idEstudiante,
                          String relacion, boolean contactoPrincipal) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + idEstudiante));

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
                        () -> vinculoRepository.save(GuardianStudent.builder()
                                .representante(representante)
                                .estudiante(estudiante)
                                .activo(true)
                                .relacion(relacion)
                                .contactoPrincipal(contactoPrincipal)
                                .build())
                );
    }

    private GuardianResponse toResponse(Guardian r) {
        List<LinkedStudentResponse> representados =
                vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(r.getIdRepresentante()).stream()
                        .map(v -> new LinkedStudentResponse(
                                v.getEstudiante().getIdEstudiante(),
                                v.getEstudiante().getPersona().getNombre() + " " + v.getEstudiante().getPersona().getApellido(),
                                v.getEstudiante().getCategoria().getNombre(),
                                v.getRelacion(),
                                v.getContactoPrincipal()))
                        .toList();

        return new GuardianResponse(
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
