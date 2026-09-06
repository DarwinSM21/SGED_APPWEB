package org.uteq.backend.academico.guardian.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.guardian.dto.GuardianPageResponse;
import org.uteq.backend.academico.guardian.dto.GuardianRequest;
import org.uteq.backend.academico.guardian.dto.GuardianResponse;
import org.uteq.backend.academico.guardian.dto.LinkRequest;
import org.uteq.backend.academico.guardian.service.GuardianService;

/**
 * CRUD de {@code Guardian}. No se comparte lectura con
 * {@code ENTRENADOR}: un coach no tiene motivo operativo para ver datos de
 * contacto de tutores. El alta la hace {@code ADMINISTRADOR} o
 * {@code RECEPCIONISTA} sobre un {@code idPersona} / {@code idUsuario} ya
 * creados vía {@code POST /api/usuarios}, opcionalmente vinculando de una
 * vez a sus representados. {@code RECEPCIONISTA} solo puede leer, crear y
 * vincular; editar o eliminar sigue siendo exclusivo de
 * {@code ADMINISTRADOR}.
 */
@RestController
@RequestMapping("/api/representantes")
@RequiredArgsConstructor
public class GuardianController {
    private final GuardianService representanteService;

    /**
     * Lista paginada de representantes.
     *
     * @param pageable paginación; por defecto 10 por página
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<GuardianPageResponse<GuardianResponse>> list(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(representanteService.list(pageable));
    }

    /**
     * Busca un representante por su identificador.
     *
     * @param id identificador del representante
     * @return {@code 200 OK} con el representante
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<GuardianResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(representanteService.findById(id));
    }

    /**
     * Registra un representante sobre una persona y una cuenta ya creadas.
     *
     * @param request persona, usuario, parentesco, contacto y representados
     *                iniciales; validado con {@code @Valid}
     * @return {@code 201 Created} con el representante registrado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la persona, el usuario o algún estudiante no existen
     *         ({@code 404})
     * @throws IllegalArgumentException si la persona o el usuario ya están
     *         asignados, o el usuario no tiene rol {@code REPRESENTANTE}
     *         ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<GuardianResponse> create(@Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(representanteService.create(request));
    }

    /**
     * Actualiza el parentesco y el teléfono de contacto de un representante.
     *
     * @param id      identificador del representante
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con el representante actualizado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<GuardianResponse> update(
            @PathVariable Long id, @Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.ok(representanteService.update(id, request));
    }

    /**
     * Reactiva un representante dado de baja.
     *
     * @param id identificador del representante
     * @return {@code 200 OK} con el representante reactivado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si ya está activo ({@code 422})
     */
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<GuardianResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(representanteService.reactivate(id));
    }

    /**
     * Baja lógica de un representante.
     *
     * @param id identificador del representante
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        representanteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vincula un estudiante a un representante. El cuerpo es opcional: sin él
     * el vínculo queda sin relación y sin contacto principal.
     *
     * @param id           identificador del representante
     * @param idEstudiante identificador del estudiante a vincular
     * @param request      relación y marca de contacto principal (opcional);
     *                     validado con {@code @Valid}
     * @return {@code 200 OK} con el representante y su lista de representados
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el representante o el estudiante no existen ({@code 404})
     */
    @PostMapping("/{id}/estudiantes/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<GuardianResponse> linkStudent(
            @PathVariable Long id, @PathVariable Long idEstudiante,
            @Valid @RequestBody(required = false) LinkRequest request) {
        return ResponseEntity.ok(representanteService.linkStudent(id, idEstudiante, request));
    }

    /**
     * Desvincula un estudiante de un representante (baja lógica del vínculo).
     *
     * @param id           identificador del representante
     * @param idEstudiante identificador del estudiante a desvincular
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si ese estudiante no está vinculado a este representante
     *         ({@code 404})
     */
    @DeleteMapping("/{id}/estudiantes/{idEstudiante}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> unlinkStudent(
            @PathVariable Long id, @PathVariable Long idEstudiante) {
        representanteService.unlinkStudent(id, idEstudiante);
        return ResponseEntity.noContent().build();
    }
}
