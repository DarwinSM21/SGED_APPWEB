package org.uteq.backend.academico.alerta.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.academico.alerta.dto.AlertaDtos.PanelAlertasResponse;
import org.uteq.backend.academico.alerta.service.AlertaService;

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
