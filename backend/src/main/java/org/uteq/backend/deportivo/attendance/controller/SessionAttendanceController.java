package org.uteq.backend.deportivo.attendance.controller;

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
import org.uteq.backend.deportivo.attendance.dto.TakeAttendanceDtos.RosterResponse;
import org.uteq.backend.deportivo.attendance.dto.TakeAttendanceDtos.TakeAttendanceRequest;
import org.uteq.backend.deportivo.attendance.service.AttendanceService;

/**
 * Lista de asistencia de una sesión, para el entrenador. Complementa el QR
 * (la vía normal, que deja el mejor dato con hora real de llegada), no lo
 * reemplaza.
 *
 * <p>{@code @Transactional} va aquí y no solo en el servicio: con
 * open-in-view deshabilitado, cualquier relación LAZY que se toque al
 * serializar la respuesta explota fuera de la transacción.
 */
@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
public class SessionAttendanceController {
    private final AttendanceService attendanceService;

    /**
     * Nómina de una sesión con el estado de asistencia de cada estudiante.
     *
     * @param idSesion identificador de la sesión
     * @return {@code 200 OK} con la nómina
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la sesión no existe ({@code 404})
     */
    @GetMapping("/sesion/{idSesion}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<RosterResponse> roster(@PathVariable Long idSesion) {
        return ResponseEntity.ok(attendanceService.roster(idSesion));
    }

    /**
     * Pasa lista de una sesión: fija el estado de asistencia de cada
     * estudiante de la nómina.
     *
     * @param idSesion identificador de la sesión
     * @param request  estado por estudiante; validado con {@code @Valid}
     * @return {@code 200 OK} con la nómina actualizada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la sesión no existe ({@code 404})
     * @throws IllegalArgumentException si la sesión aún no ocurrió o el
     *         cuerpo referencia estudiantes que no son de la categoría
     *         ({@code 422})
     */
    @PutMapping("/sesion/{idSesion}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<RosterResponse> takeAttendance(@PathVariable Long idSesion,
                                                     @Valid @RequestBody TakeAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.takeAttendance(idSesion, request));
    }
}
