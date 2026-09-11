package org.uteq.backend.seguridad.person.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.seguridad.audit.aop.Audited;
import org.uteq.backend.seguridad.auth.service.EmailVerificationService;
import org.uteq.backend.seguridad.person.dto.PersonRequest;
import org.uteq.backend.seguridad.person.dto.PersonResponse;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;

/**
 * Lógica de negocio de {@code Person}: el registro de identificación
 * (nombre, cédula, correo, fecha de nacimiento) del que dependen por clave
 * foránea estudiantes, entrenadores, representantes y usuarios. Las bajas
 * son lógicas ({@code activo = false}); la unicidad de cédula y correo se
 * valida en la capa de servicio, no solo con restricciones de base.
 */
@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personaRepository;
    private final EmailVerificationService emailVerificationService;

    /**
     * Lista paginada de personas activas.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, mapeada a {@link PersonResponse}
     */
    @Transactional(readOnly = true)
    public Page<PersonResponse> list(Pageable pageable) {
        return personaRepository.findByActivoTrue(pageable)
                .map(this::toResponse);
    }

    /**
     * Busca una persona activa por su identificador.
     *
     * @param id identificador de la persona
     * @return la persona encontrada
     * @throws ResourceNotFoundException si no existe o está inactivada
     */
    @Transactional(readOnly = true)
    public PersonResponse findById(Long id) {
        Person p = personaRepository.findByIdPersonaAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada o inactivada con id: " + id));
        return toResponse(p);
    }

    /**
     * Busca una persona activa por su número de cédula.
     *
     * @param cedula número de cédula
     * @return la persona encontrada
     * @throws ResourceNotFoundException si no existe una persona activa
     *                                      con esa cédula
     */
    @Transactional(readOnly = true)
    public PersonResponse findByCedula(String cedula) {
        Person persona = personaRepository.findByCedulaAndActivoTrue(cedula)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con cédula: " + cedula));
        return toResponse(persona);
    }

    /**
     * Registra una persona nueva.
     *
     * @param request datos de la persona a crear
     * @return la persona registrada
     * @throws IllegalArgumentException si la cédula o el correo ya están en
     *                                  uso por otra persona activa
     */
    @Audited(action = "CREAR", entity = "Persona", idSpel = "#result.idPersona",
            descriptionSpel = "'creó la persona ' + #result.nombre + ' ' + #result.apellido")
    @Transactional
    public PersonResponse create(PersonRequest request) {
        validateUniqueCedulaAndEmail(request.cedula(), request.correo(), null);

        Person persona = Person.builder()
                .name(request.nombre())
                .lastName(request.apellido())
                .nationalId(request.cedula())
                .email(request.correo())
                .phone(request.telefono())
                .photo(request.foto())
                .birthDate(request.fechaNacimiento())
                .active(true)
                .emailVerified(false)
                .build();

        persona = personaRepository.save(persona);
        // RNF-26 / H-09: el correo nace sin verificar; se dispara el doble opt-in.
        emailVerificationService.sendConfirmation(persona);
        return toResponse(persona);
    }

    /**
     * Actualiza los datos de una persona. La validación de unicidad excluye
     * a la propia persona editada.
     *
     * @param id      identificador de la persona a editar
     * @param request datos nuevos
     * @return la persona actualizada
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si la cédula o el correo
     *                                      pertenecen a otra persona
     */
    @Audited(action = "EDITAR", entity = "Persona", idSpel = "#result.idPersona",
            descriptionSpel = "'editó los datos de ' + #result.nombre + ' ' + #result.apellido")
    @Transactional
    public PersonResponse update(Long id, PersonRequest request) {
        Person persona = personaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con ID: " + id));

        validateUniqueCedulaAndEmail(request.cedula(), request.correo(), id);

        boolean correoCambio = !java.util.Objects.equals(persona.getEmail(), request.correo());

        persona.setName(request.nombre());
        persona.setLastName(request.apellido());
        persona.setNationalId(request.cedula());
        persona.setEmail(request.correo());
        persona.setPhone(request.telefono());
        persona.setPhoto(request.foto());
        persona.setBirthDate(request.fechaNacimiento());

        // RNF-26 / H-09: cambiar el correo lo deja sin verificar y dispara un
        // enlace de confirmación nuevo para la dirección nueva.
        if (correoCambio) {
            persona.setEmailVerified(false);
        }

        persona = personaRepository.save(persona);
        if (correoCambio) {
            emailVerificationService.sendConfirmation(persona);
        }
        return toResponse(persona);
    }

    /**
     * Baja lógica de una persona ({@code activo = false}); no borra la fila.
     *
     * @param id identificador de la persona
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(action = "ELIMINAR", entity = "Persona", idSpel = "#p0",
            descriptionSpel = "'desactivó la persona #' + #p0")
    @Transactional
    public void delete(Long id) {
        Person persona = personaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con ID: " + id));

        persona.setActive(false);
        personaRepository.save(persona);
    }

    // Al crear valida contra personas activas; al editar usa las consultas
    // JPQL que excluyen la fila idActual (existsAnotherPersonWith...).
    private void validateUniqueCedulaAndEmail(String cedula, String correo, Long idActual) {
        // RF-49 / H-01: la cédula es opcional; solo se comprueba unicidad si viene.
        boolean tieneCedula = cedula != null && !cedula.isBlank();
        if (idActual == null) {
            if (tieneCedula && personaRepository.existsByCedulaAndActivoTrue(cedula)) {
                throw new IllegalArgumentException("Ya existe una persona registrada con la cédula: " + cedula);
            }
            if (personaRepository.existsByCorreo(correo)) {
                throw new IllegalArgumentException("Ya existe una persona registrada con el correo: " + correo);
            }
        } else {
            if (tieneCedula && personaRepository.existsAnotherPersonWithCedula(cedula, idActual)) {
                throw new IllegalArgumentException("Ya existe una persona registrada con la cédula: " + cedula);
            }
            if (personaRepository.existsAnotherPersonWithEmail(correo, idActual)) {
                throw new IllegalArgumentException("Ya existe una persona registrada con el correo: " + correo);
            }
        }
    }

    private PersonResponse toResponse(Person p) {
        return new PersonResponse(
                p.getId(),
                p.getName(),
                p.getLastName(),
                p.getNationalId(),
                p.getEmail(),
                p.getPhone(),
                p.getPhoto(),
                p.getBirthDate(),
                p.getActive(),
                p.getCreatedAt() != null ? p.getCreatedAt().toInstant() : null
        );
    }
}
