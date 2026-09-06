package org.uteq.backend.academico.guardian.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.guardian.dto.ConsentDtos.*;
import org.uteq.backend.academico.guardian.service.ConsentService;

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
public class ConsentController {
    private final ConsentService consentimientoService;

    /**
     * Registra un consentimiento otorgado.
     *
     * @param request representante, estudiante y alcance; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el consentimiento registrado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el representante o el estudiante no existen ({@code 404})
     * @throws IllegalArgumentException si ya hay un consentimiento vigente
     *         con ese alcance ({@code 422})
     */
    @PostMapping
    public ResponseEntity<ConsentResponse> grant(@Valid @RequestBody GrantConsentRequest request) {
        String usernameAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consentimientoService.grant(request, usernameAdmin));
    }

    /**
     * Revoca un consentimiento vigente (deja constancia de quién y cuándo).
     *
     * @param id identificador del consentimiento
     * @return {@code 200 OK} con el consentimiento revocado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si ya estaba revocado ({@code 422})
     */
    @PostMapping("/{id}/revocar")
    public ResponseEntity<ConsentResponse> revoke(@PathVariable Long id) {
        String usernameAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(consentimientoService.revoke(id, usernameAdmin));
    }

    /**
     * Lista los consentimientos de un estudiante, del más reciente al más
     * antiguo.
     *
     * @param idEstudiante identificador del estudiante
     * @return {@code 200 OK} con la lista de consentimientos
     */
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<List<ConsentResponse>> listByStudent(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(consentimientoService.listByStudent(idEstudiante));
    }
}
