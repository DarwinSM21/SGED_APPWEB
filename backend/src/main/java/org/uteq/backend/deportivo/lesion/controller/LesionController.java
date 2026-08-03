package org.uteq.backend.deportivo.lesion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.lesion.dto.LesionDtos.*;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.service.LesionService;

/**
 * Lesiones. Las registra el entrenador; la recepcionista no interviene.
 *
 * <p>La descripcion es un dato de salud de un menor, asi que la lectura queda
 * restringida a entrenador y administrador. Un rol USER no tiene por que ver
 * el historial medico de nadie.
 */
@RestController
@RequestMapping("/api/lesiones")
@RequiredArgsConstructor
public class LesionController {

    private final LesionService lesionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Page<LesionResponse>> listarActivas(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lesionService.listarActivas(pageable).map(this::aResponse));
    }

    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Page<LesionResponse>> historial(
            @PathVariable Long idEstudiante,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lesionService.historialDe(idEstudiante, pageable).map(this::aResponse));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<LesionResponse> registrar(@Valid @RequestBody RegistrarLesionRequest request) {
        var lesion = lesionService.registrar(
                request.idEstudiante(), request.idEntrenador(), request.descripcion(),
                request.fechaLesion(), request.fechaEstimadaRetorno());
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(lesion));
    }

    @PostMapping("/{idLesion}/alta")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<LesionResponse> darDeAlta(@PathVariable Long idLesion,
                                                    @RequestBody(required = false) DarDeAltaRequest request) {
        var fecha = request == null ? null : request.fechaAlta();
        return ResponseEntity.ok(aResponse(lesionService.darDeAlta(idLesion, fecha)));
    }

    private LesionResponse aResponse(Lesion l) {
        var persona = l.getEstudiante().getPersona();
        return new LesionResponse(
                l.getIdLesion(),
                l.getEstudiante().getIdEstudiante(),
                persona.getNombre() + " " + persona.getApellido(),
                l.getDescripcion(),
                l.getFechaLesion(),
                l.getFechaEstimadaRetorno(),
                l.getFechaAlta(),
                l.estaActiva());
    }
}
