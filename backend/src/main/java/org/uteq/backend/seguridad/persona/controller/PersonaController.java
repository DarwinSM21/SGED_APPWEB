package org.uteq.backend.seguridad.persona.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.seguridad.persona.dto.PersonaRequest;
import org.uteq.backend.seguridad.persona.dto.PersonaResponse;
import org.uteq.backend.seguridad.persona.service.PersonaService;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping
    public ResponseEntity<Page<PersonaResponse>> listar(
            @PageableDefault(size = 10, sort = "apellido") Pageable pageable) {
        return ResponseEntity.ok(personaService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(personaService.buscarPorId(id));
    }

    @GetMapping("/cedula/{cedula}")
    public ResponseEntity<PersonaResponse> buscarPorCedula(@PathVariable String cedula) {
        return ResponseEntity.ok(personaService.buscarPorCedula(cedula));
    }

    @PostMapping
    public ResponseEntity<PersonaResponse> crear(@Valid @RequestBody PersonaRequest request) {
        PersonaResponse personaCreada = personaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(personaCreada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonaResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody PersonaRequest request) {
        return ResponseEntity.ok(personaService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}