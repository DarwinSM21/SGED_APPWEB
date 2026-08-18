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

/**
 * Lectura agregada de asistencia para el tablero. Se separa de
 * AsistenciaQrController a proposito: aquel emite y canjea tokens -escribe
 * y tiene reglas de vigencia-, este solo resume lo que ya paso.
 *
 * <p>Abierto a quien dirige la escuela y a quien entrena; no a recepcion ni
 * al representante, que no tienen por que ver el rendimiento agregado de
 * todas las categorias.
 */
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
