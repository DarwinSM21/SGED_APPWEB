package org.uteq.backend.deportivo.horario.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.horario.dto.HorarioRequest;
import org.uteq.backend.deportivo.horario.dto.HorarioResponse;
import org.uteq.backend.deportivo.horario.service.HorarioService;

import java.util.List;

/**
 * Horario fijo semanal del entrenador. Distinto de /api/sesiones: aqui se
 * define el patron recurrente ("SUB-12, Lunes y Miercoles, 16:00-18:00");
 * las filas concretas de sesiones_entrenamiento se generan solas cada dia
 * que corresponde (HorarioService.generarSesionesProgramadas(), llamado desde
 * SesionEntrenamientoController.hoy()/mias()). Una jornada que no esta en
 * el horario fijo se sigue creando a mano con POST /api/sesiones.
 */
@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
public class HorarioController {

    private final HorarioService horarioService;

    @PostMapping
    public ResponseEntity<HorarioResponse> crear(@Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.crear(usernameAutenticado(), request));
    }

    @GetMapping("/mios")
    public ResponseEntity<List<HorarioResponse>> mios() {
        return ResponseEntity.ok(horarioService.misHorarios(usernameAutenticado()));
    }

    @DeleteMapping("/{idHorario}")
    public ResponseEntity<Void> desactivar(@PathVariable Long idHorario) {
        horarioService.desactivar(usernameAutenticado(), idHorario);
        return ResponseEntity.noContent().build();
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
