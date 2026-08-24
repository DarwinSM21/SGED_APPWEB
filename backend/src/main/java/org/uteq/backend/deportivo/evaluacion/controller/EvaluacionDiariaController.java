package org.uteq.backend.deportivo.evaluacion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.*;
import org.uteq.backend.deportivo.evaluacion.dto.AlineacionDtos.AlineacionResponse;
import org.uteq.backend.deportivo.evaluacion.dto.AlineacionDtos.GuardarAlineacionRequest;
import org.uteq.backend.deportivo.evaluacion.service.AlineacionService;
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
    private final AlineacionService alineacionService;
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
     * Alineacion sugerida. El orden lo decide una regla deterministica; no
     * llama a la IA (ver {@link #feedbackPlantilla}).
     */
    @GetMapping("/sesion/{idSesion}/plantilla")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<PlantillaResponse> plantilla(@PathVariable Long idSesion) {
        return ResponseEntity.ok(plantillaService.sugerir(idSesion));
    }

    /**
     * Comentario de IA sobre la alineacion, a demanda ("Feedback IA"). Se
     * separa de {@link #plantilla} para no llamar al modelo en cada apertura
     * de la pantalla: el entrenador lo pide cuando quiere leerlo.
     */
    @PostMapping("/sesion/{idSesion}/plantilla/feedback")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<FeedbackPlantillaResponse> feedbackPlantilla(@PathVariable Long idSesion) {
        return ResponseEntity.ok(plantillaService.feedback(idSesion));
    }

    // ------------------------------------------------------------------
    // Alineacion puesta en cancha
    //
    // Distinta de /plantilla, que devuelve la sugerencia calculada. Aqui vive
    // la decision del entrenador: si guardo una, se devuelve esa; si no, la
    // sugerida. Por eso abrir una sesion pasada muestra el once real con el
    // que se jugo.
    // ------------------------------------------------------------------

    @GetMapping("/sesion/{idSesion}/alineacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<AlineacionResponse> verAlineacion(@PathVariable Long idSesion) {
        return ResponseEntity.ok(alineacionService.verDeSesion(idSesion));
    }

    @PutMapping("/sesion/{idSesion}/alineacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<AlineacionResponse> guardarAlineacion(
            @PathVariable Long idSesion,
            @Valid @RequestBody GuardarAlineacionRequest request) {
        return ResponseEntity.ok(alineacionService.guardar(idSesion, request));
    }

    /** Descarta los cambios y vuelve a la sugerencia del sistema. */
    @DeleteMapping("/sesion/{idSesion}/alineacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<AlineacionResponse> restablecerAlineacion(@PathVariable Long idSesion) {
        return ResponseEntity.ok(alineacionService.restablecer(idSesion));
    }
}
