package org.uteq.backend.deportivo.asistencia.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.asistencia.dto.AsistenciaDtos.MiHistorialResponse;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;

/**
 * Historial de asistencia del propio {@code ESTUDIANTE} autenticado
 * (complementa a {@code AsistenciaQrController.marcar}, que solo escribe).
 */
@RestController
@RequestMapping("/api/estudiante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTUDIANTE')")
public class MiAsistenciaController {
    private final AsistenciaService asistenciaService;

    /**
     * Historial de asistencia del estudiante autenticado.
     *
     * @return {@code 200 OK} con el historial
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la cuenta no tiene ficha de estudiante ({@code 404})
     */
    @GetMapping("/mi-asistencia")
    @Transactional(readOnly = true)
    public ResponseEntity<MiHistorialResponse> miHistorial() {
        return ResponseEntity.ok(asistenciaService.misAsistencias(usernameAutenticado()));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
