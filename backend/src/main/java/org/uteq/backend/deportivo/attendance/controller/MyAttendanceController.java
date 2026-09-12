package org.uteq.backend.deportivo.attendance.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.attendance.dto.AttendanceDtos.MyHistoryResponse;
import org.uteq.backend.deportivo.attendance.service.AttendanceService;

/**
 * Historial de asistencia del propio {@code ESTUDIANTE} autenticado
 * (complementa a {@code AttendanceQrController.marcar}, que solo escribe).
 */
@RestController
@RequestMapping("/api/estudiante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTUDIANTE')")
public class MyAttendanceController {
    private final AttendanceService attendanceService;

    /**
     * Historial de asistencia del estudiante autenticado.
     *
     * @return {@code 200 OK} con el historial
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la cuenta no tiene ficha de estudiante ({@code 404})
     */
    @GetMapping("/mi-asistencia")
    @Transactional(readOnly = true)
    public ResponseEntity<MyHistoryResponse> myHistory() {
        return ResponseEntity.ok(attendanceService.myAttendances(authenticatedUsername()));
    }

    private String authenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
