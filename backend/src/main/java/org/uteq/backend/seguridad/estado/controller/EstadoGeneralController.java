package org.uteq.backend.seguridad.estado.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.seguridad.estado.dto.EstadoGeneralResponse;
import org.uteq.backend.seguridad.estado.service.EstadoGeneralService;

import java.util.List;

/**
 * Catalogo de solo lectura de estados administrativos. No expone datos
 * personales, pero se restringe a usuarios autenticados con rol conocido
 * en vez de dejarse abierto a cualquier sesion valida.
 */
@RestController
@RequestMapping("/api/estados_generales")
@RequiredArgsConstructor
public class EstadoGeneralController {

    private final EstadoGeneralService estadoGeneralService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<List<EstadoGeneralResponse>> listarTodos() {
        return ResponseEntity.ok(estadoGeneralService.listarTodos());
    }
}