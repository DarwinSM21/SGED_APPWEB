package org.uteq.backend.deportivo.asistencia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.asistencia.dto.PasarListaDtos.NominaResponse;
import org.uteq.backend.deportivo.asistencia.dto.PasarListaDtos.PasarListaRequest;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;

@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
public class AsistenciaSesionController {
    private final AsistenciaService asistenciaService;

    @GetMapping("/sesion/{idSesion}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<NominaResponse> nomina(@PathVariable Long idSesion) {
        return ResponseEntity.ok(asistenciaService.nomina(idSesion));
    }

    @PutMapping("/sesion/{idSesion}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<NominaResponse> pasarLista(@PathVariable Long idSesion,
                                                     @Valid @RequestBody PasarListaRequest request) {
        return ResponseEntity.ok(asistenciaService.pasarLista(idSesion, request));
    }
}
