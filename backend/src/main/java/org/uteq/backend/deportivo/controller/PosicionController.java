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
import org.uteq.backend.deportivo.entity.Posicion;
import org.uteq.backend.deportivo.repository.PosicionRepository;

import java.util.List;

@RestController
@RequestMapping("/api/posiciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posiciones", description = "Gestión de posiciones de juego")
@SecurityRequirement(name = "bearer-jwt")
public class PosicionController {

    private final PosicionRepository posicionRepository;

    /**
     * Obtener todas las posiciones activas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Listar posiciones",
               description = "Obtiene todas las posiciones de juego disponibles")
    public ResponseEntity<List<Posicion>> obtenerPosiciones() {
        
        log.info("Obteniendo posiciones");
        List<Posicion> posiciones = posicionRepository.findByActivoTrue();
        return ResponseEntity.ok(posiciones);
    }
}
