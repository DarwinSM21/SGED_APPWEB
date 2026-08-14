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
 * Historial de asistencia del propio ESTUDIANTE autenticado. Antes de esto
 * solo podia marcar asistencia (AsistenciaQrController.marcar); no tenia
 * forma de consultar lo que ya habia marcado.
 */
@RestController
@RequestMapping("/api/estudiante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTUDIANTE')")
public class MiAsistenciaController {

    private final AsistenciaService asistenciaService;

    @GetMapping("/mi-asistencia")
    @Transactional(readOnly = true)
    public ResponseEntity<MiHistorialResponse> miHistorial() {
        return ResponseEntity.ok(asistenciaService.misAsistencias(usernameAutenticado()));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
