package org.uteq.backend.deportivo.asistencia.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.asistencia.dto.AsistenciaDtos.MapaAsistenciaResponse;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;

@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
public class ResumenAsistenciaController {
    private final AsistenciaService asistenciaService;

    @GetMapping("/mapa")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<MapaAsistenciaResponse> mapa(@RequestParam(defaultValue = "35") int dias) {
        return ResponseEntity.ok(asistenciaService.mapaDeAsistencia(dias));
    }
}
