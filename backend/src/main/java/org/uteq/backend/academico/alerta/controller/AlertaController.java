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
 * Panel de estudiantes que requieren atención. Restringido a
 * {@code ADMINISTRADOR} y {@code RECEPCIONISTA} porque incluye estado de
 * pago: un entrenador necesita saber quién falta o está lesionado, pero no
 * quién debe la cuota.
 */
@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {
    private final AlertaService alertaService;

    /**
     * Panel operativo del día: contadores por tipo de alerta y el detalle de
     * los estudiantes más urgentes.
     *
     * @return {@code 200 OK} con el panel de alertas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<PanelAlertasResponse> panel() {
        return ResponseEntity.ok(alertaService.panel());
    }
}
