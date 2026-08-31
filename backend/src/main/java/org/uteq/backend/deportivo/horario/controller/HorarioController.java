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

    @PutMapping("/{idHorario}")
    public ResponseEntity<HorarioResponse> editar(@PathVariable Long idHorario,
                                                  @Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.ok(horarioService.editar(usernameAutenticado(), idHorario, request));
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
