package org.uteq.backend.deportivo.evaluacion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.*;
import org.uteq.backend.deportivo.evaluacion.service.EvaluacionDiariaService;
import org.uteq.backend.deportivo.evaluacion.service.PlantillaService;

/**
 * Evaluacion diaria y sugerencia de alineacion.
 *
 * <p>Todos los endpoints exigen rol ENTRENADOR o ADMINISTRADOR. Se anota desde
 * el primer commit y no despues: los cinco recursos que la reestructuracion
 * agrego sin {@code @PreAuthorize} dejaron accesibles datos de menores a
 * cualquier cuenta autenticada (hallazgo H-08). No se repite.
 */
@RestController
@RequestMapping("/api/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionDiariaController {

    private final EvaluacionDiariaService evaluacionService;
    private final PlantillaService plantillaService;

    /** Abre la pantalla de evaluacion de una sesion, con precarga y bloqueos. */
    @GetMapping("/sesion/{idSesion}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<EvaluacionSesionResponse> abrir(@PathVariable Long idSesion) {
        return ResponseEntity.ok(evaluacionService.abrir(idSesion));
    }

    /**
     * Autoguardado de un jugador. La interfaz lo llama cada vez que el
     * entrenador suelta un slider, de modo que se invoca con frecuencia y es
     * idempotente: reescribe en vez de acumular.
     */
    @PutMapping("/sesion/{idSesion}/jugadores")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Void> guardarJugador(@PathVariable Long idSesion,
                                               @Valid @RequestBody GuardarJugadorRequest request) {
        evaluacionService.guardarJugador(idSesion, request);
        return ResponseEntity.noContent().build();
    }

    /** Cierra la evaluacion. A partir de aqui no admite cambios. */
    @PostMapping("/sesion/{idSesion}/finalizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Void> finalizar(@PathVariable Long idSesion,
                                          @RequestBody(required = false) String observacionGeneral) {
        evaluacionService.finalizar(idSesion, observacionGeneral);
        return ResponseEntity.noContent().build();
    }

    /**
     * Alineacion sugerida. El orden lo decide una regla deterministica; el
     * modelo de lenguaje solo comenta el resultado.
     */
    @GetMapping("/sesion/{idSesion}/plantilla")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<PlantillaResponse> plantilla(@PathVariable Long idSesion) {
        return ResponseEntity.ok(plantillaService.sugerir(idSesion));
    }
}
