package org.uteq.backend.deportivo.position.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.position.dto.PositionResponse;
import org.uteq.backend.deportivo.position.repository.PositionRepository;

import java.util.List;

/**
 * Catálogo de posiciones de juego (POR, DFC, LD…), de solo lectura desde la
 * aplicación (sin alta ni edición). Lo necesita el formulario de ficha de
 * estudiante para asignar la posición nominal de cada jugador.
 */
@RestController
@RequestMapping("/api/posiciones")
@RequiredArgsConstructor
public class PositionController {
    private final PositionRepository positionRepository;

    /**
     * Lista las posiciones activas, ordenadas por identificador.
     *
     * @return {@code 200 OK} con las posiciones activas
     */
    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<PositionResponse>> findActive() {
        List<PositionResponse> posiciones = positionRepository.findActiveOrderByIdAsc().stream()
                .map(p -> new PositionResponse(p.getIdPosicion(), p.getNombre(), p.getAbreviatura()))
                .toList();
        return ResponseEntity.ok(posiciones);
    }
}
