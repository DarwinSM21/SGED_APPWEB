package org.uteq.backend.deportivo.attendance.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.attendance.dto.AttendanceDtos.AttendanceMapResponse;
import org.uteq.backend.deportivo.attendance.service.AttendanceService;

/**
 * Lectura agregada de asistencia para el tablero. Se separa de
 * {@code AttendanceQrController} a propósito: aquel emite y canjea tokens,
 * este solo resume lo que ya pasó. Abierto a quien dirige la escuela y a
 * quien entrena; no a recepción ni al representante.
 */
@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
public class AttendanceSummaryController {
    private final AttendanceService attendanceService;

    /**
     * Mapa de calor de asistencia de los últimos días, por categoría.
     *
     * @param dias ventana en días hacia atrás; por defecto 35
     * @return {@code 200 OK} con el mapa de asistencia
     */
    @GetMapping("/mapa")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<AttendanceMapResponse> map(@RequestParam(defaultValue = "35") int dias) {
        return ResponseEntity.ok(attendanceService.attendanceMap(dias));
    }
}
