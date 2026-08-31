package org.uteq.backend.deportivo.evaluacion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.*;
import org.uteq.backend.deportivo.evaluacion.service.EvaluacionDiariaService;

@RestController
@RequestMapping("/api/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionDiariaController {
    private final EvaluacionDiariaService evaluacionService;

    @GetMapping("/sesion/{idSesion}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<EvaluacionSesionResponse> abrir(@PathVariable Long idSesion) {
        return ResponseEntity.ok(evaluacionService.abrir(idSesion));
    }

    @PutMapping("/sesion/{idSesion}/jugadores")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Void> guardarJugador(@PathVariable Long idSesion,
                                               @Valid @RequestBody GuardarJugadorRequest request) {
        evaluacionService.guardarJugador(idSesion, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sesion/{idSesion}/finalizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Void> finalizar(@PathVariable Long idSesion,
                                          @RequestBody(required = false) String observacionGeneral) {
        evaluacionService.finalizar(idSesion, observacionGeneral);
        return ResponseEntity.noContent().build();
    }
}
