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

@Service
@RequiredArgsConstructor
public class PersonaService {
    private final PersonaRepository personaRepository;

    @Transactional(readOnly = true)
    public Page<PersonaResponse> listar(Pageable pageable) {
        return personaRepository.findByActivoTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PersonaResponse buscarPorId(Long id) {
        Persona p = personaRepository.findByIdPersonaAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada o inactivada con id: " + id));
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public PersonaResponse buscarPorCedula(String cedula) {
        Persona persona = personaRepository.findByCedulaAndActivoTrue(cedula)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con cédula: " + cedula));
        return toResponse(persona);
    }

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

    @Auditado(accion = "ELIMINAR", entidad = "Persona", idSpel = "#p0",
            descripcionSpel = "'desactivó la persona #' + #p0")
    @Transactional
    public void eliminar(Long id) {
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + id));

        persona.setActivo(false);
        personaRepository.save(persona);
    }

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
