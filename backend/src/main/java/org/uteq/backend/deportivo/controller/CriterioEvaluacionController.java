package org.uteq.backend.deportivo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.entity.CriterioEvaluacion;
import org.uteq.backend.deportivo.repository.CriterioEvaluacionRepository;

import java.util.List;

@RestController
@RequestMapping("/api/criterios-evaluacion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Criterios de Evaluación", description = "Gestión de criterios para evaluaciones")
@SecurityRequirement(name = "bearer-jwt")
public class CriterioEvaluacionController {

    private final CriterioEvaluacionRepository criterioRepository;

    /**
     * Obtener todos los criterios de evaluación activos
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Listar criterios",
               description = "Obtiene todos los criterios de evaluación disponibles")
    public ResponseEntity<List<CriterioEvaluacion>> obtenerCriterios() {
        
        log.info("Obteniendo criterios de evaluación");
        List<CriterioEvaluacion> criterios = criterioRepository.findByActivoTrue();
        return ResponseEntity.ok(criterios);
    }
}
