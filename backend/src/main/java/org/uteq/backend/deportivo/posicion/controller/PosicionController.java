package org.uteq.backend.deportivo.posicion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.posicion.dto.PosicionResponse;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;

import java.util.List;

/**
 * Catalogo de posiciones de juego (POR, DFC, LD... ver Posicion.java: es de
 * solo lectura desde la aplicacion, no tiene alta/edicion). Mismos roles de
 * lectura que /api/categorias/activas y /api/especialidades/activas: lo
 * necesita el formulario de ficha de estudiante para asignar la posicion
 * nominal de cada jugador.
 */
@RestController
@RequestMapping("/api/posiciones")
@RequiredArgsConstructor
public class PosicionController {

    private final PosicionRepository posicionRepository;

    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<PosicionResponse>> listarActivas() {
        List<PosicionResponse> posiciones = posicionRepository.findByActivoTrueOrderByIdPosicionAsc().stream()
                .map(p -> new PosicionResponse(p.getIdPosicion(), p.getNombre(), p.getAbreviatura()))
                .toList();
        return ResponseEntity.ok(posiciones);
    }
}
