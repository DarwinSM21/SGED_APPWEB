package org.uteq.backend.seguridad.persona.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.seguridad.persona.dto.PersonaRequest;
import org.uteq.backend.seguridad.persona.dto.PersonaResponse;
import org.uteq.backend.seguridad.persona.service.PersonaService;

/**
 * CRUD de {@code Persona}. Concentra los datos identificativos (cédula,
 * correo) de estudiantes menores de edad, por lo que casi todos los
 * endpoints quedan restringidos a {@code ADMINISTRADOR}. La excepción es
 * {@link #crear}: la recepcionista también la necesita, porque toda
 * {@code Estudiante} cuelga de una {@code Persona} ya existente y ese es el
 * primer paso del alta.
 */
@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {
    private final PersonaService personaService;

    /**
     * Lista paginada de personas activas.
     *
     * @param pageable paginación y orden; por defecto 10 por página, orden
     *                 por {@code apellido}
     * @return {@code 200 OK} con la página solicitada
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<PersonaResponse>> listar(
            @PageableDefault(size = 10, sort = "apellido") Pageable pageable) {
        return ResponseEntity.ok(personaService.listar(pageable));
    }

    /**
     * Busca una persona activa por su identificador.
     *
     * @param id identificador de la persona
     * @return {@code 200 OK} con la persona
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe o está inactivada ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PersonaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(personaService.buscarPorId(id));
    }

    /**
     * Busca una persona activa por su número de cédula.
     *
     * @param cedula número de cédula
     * @return {@code 200 OK} con la persona
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe una persona activa con esa cédula ({@code 404})
     */
    @GetMapping("/cedula/{cedula}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PersonaResponse> buscarPorCedula(@PathVariable String cedula) {
        return ResponseEntity.ok(personaService.buscarPorCedula(cedula));
    }

    /**
     * Registra una persona nueva.
     *
     * @param request datos de la persona; validado con {@code @Valid}
     * @return {@code 201 Created} con la persona registrada
     * @throws IllegalArgumentException si la cédula o el correo ya están en
     *                                  uso ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<PersonaResponse> crear(@Valid @RequestBody PersonaRequest request) {
        PersonaResponse personaCreada = personaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(personaCreada);
    }

    /**
     * Actualiza los datos de una persona.
     *
     * @param id      identificador de la persona a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con la persona actualizada
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si la cédula o el correo pertenecen a
     *                                  otra persona ({@code 422})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PersonaResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody PersonaRequest request) {
        return ResponseEntity.ok(personaService.editar(id, request));
    }

    /**
     * Baja lógica de una persona ({@code activo = false}).
     *
     * @param id identificador de la persona
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
