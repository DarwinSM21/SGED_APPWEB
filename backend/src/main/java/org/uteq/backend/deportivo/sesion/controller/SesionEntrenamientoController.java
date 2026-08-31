package org.uteq.backend.deportivo.sesion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.sesion.dto.SesionCrearRequest;
import org.uteq.backend.deportivo.sesion.dto.SesionHistorialResponse;
import org.uteq.backend.deportivo.sesion.dto.SesionHoyResponse;
import org.uteq.backend.deportivo.sesion.service.SesionEntrenamientoService;

import java.util.List;

@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionEntrenamientoController {
    private final SesionEntrenamientoService sesionService;

    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<SesionHoyResponse>> hoy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean veTodasLasSesiones = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")
                        || a.getAuthority().equals("ROLE_RECEPCIONISTA"));

        return ResponseEntity.ok(sesionService.sesionesDeHoy(auth.getName(), veTodasLasSesiones));
    }

    @GetMapping("/mias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<List<SesionHoyResponse>> mias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean veTodasLasSesiones = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        return ResponseEntity.ok(sesionService.misSesiones(auth.getName(), veTodasLasSesiones, page, size));
    }

    @GetMapping("/{idSesion}/historial")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<SesionHistorialResponse> historial(@PathVariable Long idSesion) {
        return ResponseEntity.ok(sesionService.historial(idSesion));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<SesionHoyResponse> crear(@Valid @RequestBody SesionCrearRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SesionHoyResponse creada = sesionService.crear(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
