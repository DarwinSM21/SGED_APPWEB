package org.uteq.backend.academico.representante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.representante.dto.ConsentimientoDtos.*;
import org.uteq.backend.academico.representante.service.ConsentimientoService;

import java.util.List;

@RestController
@RequestMapping("/api/consentimientos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConsentimientoController {
    private final ConsentimientoService consentimientoService;

    @PostMapping
    public ResponseEntity<ConsentimientoResponse> otorgar(@Valid @RequestBody OtorgarConsentimientoRequest request) {
        String usernameAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consentimientoService.otorgar(request, usernameAdmin));
    }

    @PostMapping("/{id}/revocar")
    public ResponseEntity<ConsentimientoResponse> revocar(@PathVariable Long id) {
        String usernameAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(consentimientoService.revocar(id, usernameAdmin));
    }

    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<List<ConsentimientoResponse>> listarPorEstudiante(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(consentimientoService.listarPorEstudiante(idEstudiante));
    }
}
