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
