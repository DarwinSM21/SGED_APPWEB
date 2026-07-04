package org.uteq.backend.deportivo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.dto.AsistenciaRequest;
import org.uteq.backend.deportivo.dto.AsistenciaResponse;
import org.uteq.backend.deportivo.service.AsistenciaService;

import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Asistencias", description = "Gestión de asistencias en entrenamientos")
@SecurityRequirement(name = "bearer-jwt")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    /**
     * Registrar o actualizar asistencia de un estudiante
     */
    @PostMapping("/sesion/{idSesion}/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Registrar asistencia de estudiante",
               description = "Registra o actualiza la asistencia de un estudiante en una sesión")
    public ResponseEntity<AsistenciaResponse> registrarAsistencia(
            @PathVariable Long idSesion,
            @PathVariable Long idEstudiante,
            @RequestBody AsistenciaRequest request) {
        
        log.info("Registrando asistencia: Sesión={}, Estudiante={}", idSesion, idEstudiante);
        AsistenciaResponse response = asistenciaService.registrarAsistencia(idSesion, idEstudiante, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener todas las asistencias de una sesión
     */
    @GetMapping("/sesion/{idSesion}")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Obtener asistencias de sesión",
               description = "Obtiene todas las asistencias registradas en una sesión")
    public ResponseEntity<List<AsistenciaResponse>> obtenerAsistenciasPorSesion(
            @PathVariable Long idSesion) {
        
        log.info("Obteniendo asistencias de sesión: {}", idSesion);
        List<AsistenciaResponse> asistencias = asistenciaService.obtenerAsistenciasPorSesion(idSesion);
        return ResponseEntity.ok(asistencias);
    }

    /**
     * Obtener historial de asistencias de un estudiante (paginado)
     */
    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Historial de asistencias de estudiante",
               description = "Obtiene el historial paginado de asistencias de un estudiante")
    public ResponseEntity<Page<AsistenciaResponse>> obtenerHistoricoEstudiante(
            @PathVariable Long idEstudiante,
            @Parameter(description = "Número de página (0-indexed), tamaño, ordenamiento")
            Pageable pageable) {
        
        log.info("Obteniendo historial de asistencias: Estudiante={}", idEstudiante);
        Page<AsistenciaResponse> historial = asistenciaService.obtenerHistoricoEstudiante(idEstudiante, pageable);
        return ResponseEntity.ok(historial);
    }

    /**
     * Obtener asistencia específica
     */
    @GetMapping("/{idAsistencia}")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Obtener asistencia específica",
               description = "Obtiene los detalles de una asistencia")
    public ResponseEntity<AsistenciaResponse> obtenerAsistencia(
            @PathVariable Long idAsistencia) {
        
        log.info("Obteniendo asistencia: {}", idAsistencia);
        AsistenciaResponse asistencia = asistenciaService.obtenerAsistencia(idAsistencia);
        return ResponseEntity.ok(asistencia);
    }
}
