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
import org.uteq.backend.deportivo.dto.DetalleEvaluacionRequest;
import org.uteq.backend.deportivo.dto.EvaluacionDiariaRequest;
import org.uteq.backend.deportivo.dto.EvaluacionDiariaResponse;
import org.uteq.backend.deportivo.service.EvaluacionDiariaService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/evaluaciones-diarias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Evaluaciones Diarias", description = "Gestión de evaluaciones de rendimiento diario")
@SecurityRequirement(name = "bearer-jwt")
public class EvaluacionDiariaController {

    private final EvaluacionDiariaService evaluacionService;

    /**
     * Crear nueva evaluación diaria (en estado BORRADOR)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Crear evaluación diaria",
               description = "Crea una nueva evaluación diaria en estado BORRADOR")
    public ResponseEntity<EvaluacionDiariaResponse> crearEvaluacion(
            @RequestBody EvaluacionDiariaRequest request) {
        
        log.info("Creando evaluación diaria: Sesión={}", request.getIdSesion());
        EvaluacionDiariaResponse response = evaluacionService.crearEvaluacion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Agregar calificación a un estudiante en la evaluación
     * REGLA: Solo se pueden calificar si tienen asistencia PRESENTE o TARDE
     */
    @PostMapping("/{idEvaluacion}/estudiante/{idEstudiante}/calificacion")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Agregar calificación a estudiante",
               description = "Agrega una calificación a un estudiante en una evaluación. " +
                           "REGLA: Solo se puede calificar si el estudiante tiene asistencia PRESENTE o TARDE")
    public ResponseEntity<Void> agregarCalificacion(
            @PathVariable Long idEvaluacion,
            @PathVariable Long idEstudiante,
            @RequestBody DetalleEvaluacionRequest request) {
        
        log.info("Agregando calificación: Eval={}, Est={}, Criterio={}, Puntaje={}",
                idEvaluacion, idEstudiante, request.getIdCriterio(), request.getPuntaje());
        
        evaluacionService.agregarCalificacion(idEvaluacion, idEstudiante, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Finalizar evaluación (cambiar a estado FINALIZADA)
     */
    @PutMapping("/{idEvaluacion}/finalizar")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Finalizar evaluación",
               description = "Finaliza una evaluación, pasando su estado de BORRADOR a FINALIZADA")
    public ResponseEntity<EvaluacionDiariaResponse> finalizarEvaluacion(
            @PathVariable Long idEvaluacion) {
        
        log.info("Finalizando evaluación: {}", idEvaluacion);
        EvaluacionDiariaResponse response = evaluacionService.finalizarEvaluacion(idEvaluacion);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener evaluación específica
     */
    @GetMapping("/{idEvaluacion}")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Obtener evaluación",
               description = "Obtiene los detalles de una evaluación específica")
    public ResponseEntity<EvaluacionDiariaResponse> obtenerEvaluacion(
            @PathVariable Long idEvaluacion) {
        
        log.info("Obteniendo evaluación: {}", idEvaluacion);
        EvaluacionDiariaResponse evaluacion = evaluacionService.obtenerEvaluacion(idEvaluacion);
        return ResponseEntity.ok(evaluacion);
    }

    /**
     * Obtener evaluaciones por entrenador (paginado)
     */
    @GetMapping("/entrenador/{idEntrenador}")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Evaluaciones de entrenador",
               description = "Obtiene las evaluaciones diarias creadas por un entrenador")
    public ResponseEntity<Page<EvaluacionDiariaResponse>> obtenerEvaluacionesPorEntrenador(
            @PathVariable Long idEntrenador,
            @Parameter(description = "Número de página (0-indexed), tamaño, ordenamiento")
            Pageable pageable) {
        
        log.info("Obteniendo evaluaciones del entrenador: {}", idEntrenador);
        Page<EvaluacionDiariaResponse> evaluaciones = evaluacionService.obtenerEvaluacionesPorEntrenador(idEntrenador, pageable);
        return ResponseEntity.ok(evaluaciones);
    }

    /**
     * Obtener promedio de evaluación de un estudiante
     */
    @GetMapping("/{idEvaluacion}/estudiante/{idEstudiante}/promedio")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Obtener promedio de estudiante",
               description = "Obtiene el promedio de calificaciones de un estudiante en una evaluación")
    public ResponseEntity<BigDecimal> obtenerPromedioEstudiante(
            @PathVariable Long idEvaluacion,
            @PathVariable Long idEstudiante) {
        
        log.info("Obteniendo promedio: Eval={}, Est={}", idEvaluacion, idEstudiante);
        BigDecimal promedio = evaluacionService.obtenerPromedioEstudiante(idEvaluacion, idEstudiante);
        return ResponseEntity.ok(promedio);
    }
}
