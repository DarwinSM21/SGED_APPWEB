package org.uteq.backend.academico.alerta.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.academico.alerta.dto.AlertaDtos.PanelAlertasResponse;
import org.uteq.backend.academico.alerta.service.AlertaService;

/**
 * Panel de estudiantes que requieren atencion.
 *
 * <p>Restringido a ADMINISTRADOR y RECEPCIONISTA porque incluye estado de
 * pago: un entrenador necesita saber quien falta o esta lesionado, pero no
 * quien debe la cuota. Abrirlo a ENTRENADOR obligaria a devolver una
 * version recortada, no a sumar un rol mas a esta lista.
 */
@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<PanelAlertasResponse> panel() {
        return ResponseEntity.ok(alertaService.panel());
    }
}
