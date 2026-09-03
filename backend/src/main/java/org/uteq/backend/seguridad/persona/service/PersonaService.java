package org.uteq.backend.seguridad.persona.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.persona.dto.PersonaRequest;
import org.uteq.backend.seguridad.persona.dto.PersonaResponse;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;

/**
 * Lógica de negocio de {@code Persona}: el registro de identificación
 * (nombre, cédula, correo, fecha de nacimiento) del que dependen por clave
 * foránea estudiantes, entrenadores, representantes y usuarios. Las bajas
 * son lógicas ({@code activo = false}); la unicidad de cédula y correo se
 * valida en la capa de servicio, no solo con restricciones de base.
 */
@Service
@RequiredArgsConstructor
public class PersonaService {
    private final PersonaRepository personaRepository;

    /**
     * Lista paginada de personas activas.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, mapeada a {@link PersonaResponse}
     */
    @Transactional(readOnly = true)
    public Page<PersonaResponse> listar(Pageable pageable) {
        return personaRepository.findByActivoTrue(pageable)
                .map(this::toResponse);
    }

    /**
     * Busca una persona activa por su identificador.
     *
     * @param id identificador de la persona
     * @return la persona encontrada
     * @throws RecursoNoEncontradoException si no existe o está inactivada
     */
    @Transactional(readOnly = true)
    public PersonaResponse buscarPorId(Long id) {
        Persona p = personaRepository.findByIdPersonaAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada o inactivada con id: " + id));
        return toResponse(p);
    }

    /**
     * Busca una persona activa por su número de cédula.
     *
     * @param cedula número de cédula
     * @return la persona encontrada
     * @throws RecursoNoEncontradoException si no existe una persona activa
     *                                      con esa cédula
     */
    @Transactional(readOnly = true)
    public PersonaResponse buscarPorCedula(String cedula) {
        Persona persona = personaRepository.findByCedulaAndActivoTrue(cedula)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con cédula: " + cedula));
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
    @Auditado(accion = "CREAR", entidad = "Persona", idSpel = "#result.idPersona",
            descripcionSpel = "'creó la persona ' + #result.nombre + ' ' + #result.apellido")
    @Transactional
    public PersonaResponse crear(PersonaRequest request) {
        validarUnicidadCedulaYCorreo(request.cedula(), request.correo(), null);

        Persona persona = Persona.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .cedula(request.cedula())
                .correo(request.correo())
                .telefono(request.telefono())
                .foto(request.foto())
                .fechaNacimiento(request.fechaNacimiento())
                .activo(true)
                .build();

        persona = personaRepository.save(persona);
        return toResponse(persona);
    }

    /**
     * Actualiza los datos de una persona. La validación de unicidad excluye
     * a la propia persona editada.
     *
     * @param id      identificador de la persona a editar
     * @param request datos nuevos
     * @return la persona actualizada
     * @throws RecursoNoEncontradoException si no existe
     * @throws IllegalArgumentException     si la cédula o el correo
     *                                      pertenecen a otra persona
     */
    @Auditado(accion = "EDITAR", entidad = "Persona", idSpel = "#result.idPersona",
            descripcionSpel = "'editó los datos de ' + #result.nombre + ' ' + #result.apellido")
    @Transactional
    public PersonaResponse editar(Long id, PersonaRequest request) {
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + id));

        validarUnicidadCedulaYCorreo(request.cedula(), request.correo(), id);

        persona.setNombre(request.nombre());
        persona.setApellido(request.apellido());
        persona.setCedula(request.cedula());
        persona.setCorreo(request.correo());
        persona.setTelefono(request.telefono());
        persona.setFoto(request.foto());
        persona.setFechaNacimiento(request.fechaNacimiento());

        persona = personaRepository.save(persona);
        return toResponse(persona);
    }

    /**
     * Baja lógica de una persona ({@code activo = false}); no borra la fila.
     *
     * @param id identificador de la persona
     * @throws RecursoNoEncontradoException si no existe
     */
    @Auditado(accion = "ELIMINAR", entidad = "Persona", idSpel = "#p0",
            descripcionSpel = "'desactivó la persona #' + #p0")
    @Transactional
    public void eliminar(Long id) {
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + id));

        persona.setActivo(false);
        personaRepository.save(persona);
    }

    // Al crear valida contra personas activas; al editar usa las consultas
    // JPQL que excluyen la fila idActual (existeOtraPersonaCon...).
    private void validarUnicidadCedulaYCorreo(String cedula, String correo, Long idActual) {
        if (idActual == null) {
            if (personaRepository.existsByCedulaAndActivoTrue(cedula)) {
                throw new IllegalArgumentException("Ya existe una persona registrada con la cédula: " + cedula);
            }
            if (personaRepository.existsByCorreo(correo)) {
                throw new IllegalArgumentException("Ya existe una persona registrada con el correo: " + correo);
            }
        } else {
            if (personaRepository.existeOtraPersonaConCedula(cedula, idActual)) {
                throw new IllegalArgumentException("Ya existe una persona registrada con la cédula: " + cedula);
            }
            if (personaRepository.existeOtraPersonaConCorreo(correo, idActual)) {
                throw new IllegalArgumentException("Ya existe una persona registrada con el correo: " + correo);
            }
        }
    }

    private PersonaResponse toResponse(Persona p) {
        return new PersonaResponse(
                p.getIdPersona(),
                p.getNombre(),
                p.getApellido(),
                p.getCedula(),
                p.getCorreo(),
                p.getTelefono(),
                p.getFoto(),
                p.getFechaNacimiento(),
                p.getActivo(),
                p.getCreatedAt() != null ? p.getCreatedAt().toInstant() : null
        );
    }
}
