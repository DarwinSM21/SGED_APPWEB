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

/**
 * Lista de asistencia de una sesion, para el entrenador.
 *
 * <p>Complementa el QR, no lo reemplaza: el QR sigue siendo la via normal
 * -y la que deja el mejor dato, con hora real de llegada-, pero deja de ser
 * la unica, que era el problema.
 *
 * <p>El @Transactional va aqui y no solo en el servicio: con
 * open-in-view: false, cualquier relacion LAZY que se toque al serializar la
 * respuesta explota fuera de la transaccion. Ya paso en LesionController y en
 * SesionEntrenamientoController.
 */
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
