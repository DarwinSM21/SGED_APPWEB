package org.uteq.backend.deportivo.sesion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.sesion.dto.SesionCrearRequest;
import org.uteq.backend.deportivo.sesion.dto.SesionHoyResponse;
import org.uteq.backend.deportivo.sesion.service.SesionEntrenamientoService;

import java.util.List;

/**
 * Punto de entrada del entrenador (y, desde RECEPCIONISTA, de la pantalla de
 * QR) a sus sesiones: cuales hay hoy, el historial completo, y el alta de
 * una nueva.
 *
 * <p>Sin /hoy, la pantalla de evaluacion diaria (que exige un id de sesion en
 * la ruta) no tiene forma de descubrirse a si misma: el entrenador tendria
 * que conocer el numero de antemano. Lo mismo le pasaria a recepcion para
 * elegir para cual sesion mostrar el QR.
 *
 * <p>La logica de negocio vive en SesionEntrenamientoService (D-03 / R-03 del
 * informe de evaluacion de calidad): este controlador solo resuelve la
 * identidad autenticada (SecurityContextHolder) y traduce HTTP a llamadas
 * de dominio.
 */
@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionEntrenamientoController {

    private final SesionEntrenamientoService sesionService;

    /**
     * Un ADMINISTRADOR o RECEPCIONISTA ve todas las sesiones de hoy (el
     * recepcionista necesita elegir cualquiera de ellas para mostrar su QR,
     * no esta atado a un entrenador concreto); un ENTRENADOR solo las suyas,
     * resuelto desde su propio usuario autenticado y no desde un parametro
     * que el cliente pudiera manipular para ver la agenda de otro.
     */
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
    @PreAuthorize("hasRole('ENTRENADOR')")
    public ResponseEntity<List<SesionHoyResponse>> mias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(sesionService.misSesiones(username, page, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('ENTRENADOR')")
    public ResponseEntity<SesionHoyResponse> crear(@Valid @RequestBody SesionCrearRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SesionHoyResponse creada = sesionService.crear(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
