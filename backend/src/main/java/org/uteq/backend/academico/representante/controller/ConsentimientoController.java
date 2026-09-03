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

/**
 * Administra el consentimiento del representante para el tratamiento de
 * datos de un representado (hallazgo H-04 de {@code ETHICS.md}). Solo
 * {@code ADMINISTRADOR}: en esta iteración el representante no lo otorga
 * desde la app, lo registra un administrador.
 */
@RestController
@RequestMapping("/api/consentimientos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConsentimientoController {
    private final ConsentimientoService consentimientoService;

    /**
     * Registra un consentimiento otorgado.
     *
     * @param request representante, estudiante y alcance; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el consentimiento registrado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el representante o el estudiante no existen ({@code 404})
     * @throws IllegalArgumentException si ya hay un consentimiento vigente
     *         con ese alcance ({@code 422})
     */
    @PostMapping
    public ResponseEntity<ConsentimientoResponse> otorgar(@Valid @RequestBody OtorgarConsentimientoRequest request) {
        String usernameAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consentimientoService.otorgar(request, usernameAdmin));
    }

    /**
     * Revoca un consentimiento vigente (deja constancia de quién y cuándo).
     *
     * @param id identificador del consentimiento
     * @return {@code 200 OK} con el consentimiento revocado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si ya estaba revocado ({@code 422})
     */
    @PostMapping("/{id}/revocar")
    public ResponseEntity<ConsentimientoResponse> revocar(@PathVariable Long id) {
        String usernameAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(consentimientoService.revocar(id, usernameAdmin));
    }

    /**
     * Lista los consentimientos de un estudiante, del más reciente al más
     * antiguo.
     *
     * @param idEstudiante identificador del estudiante
     * @return {@code 200 OK} con la lista de consentimientos
     */
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<List<ConsentimientoResponse>> listarPorEstudiante(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(consentimientoService.listarPorEstudiante(idEstudiante));
    }
}
