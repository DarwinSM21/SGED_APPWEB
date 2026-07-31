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
 * CRUD de Persona. Concentra los datos identificativos (cedula, correo)
 * de estudiantes menores de edad, por lo que todos los endpoints quedan
 * restringidos a ADMINISTRADOR: no hay ninguna operacion de este recurso
 * que un rol ENTRENADOR o USER necesite ejercer directamente.
 */
@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<PersonaResponse>> listar(
            @PageableDefault(size = 10, sort = "apellido") Pageable pageable) {
        return ResponseEntity.ok(personaService.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PersonaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(personaService.buscarPorId(id));
    }

    @GetMapping("/cedula/{cedula}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PersonaResponse> buscarPorCedula(@PathVariable String cedula) {
        return ResponseEntity.ok(personaService.buscarPorCedula(cedula));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PersonaResponse> crear(@Valid @RequestBody PersonaRequest request) {
        PersonaResponse personaCreada = personaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(personaCreada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PersonaResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody PersonaRequest request) {
        return ResponseEntity.ok(personaService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}