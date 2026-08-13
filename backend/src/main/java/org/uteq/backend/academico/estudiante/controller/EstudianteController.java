package org.uteq.backend.academico.estudiante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.estudiante.dto.EstudiantePageResponse;
import org.uteq.backend.academico.estudiante.dto.EstudianteRequest;
import org.uteq.backend.academico.estudiante.dto.EstudianteResponse;
import org.uteq.backend.academico.estudiante.dto.HabilitarAccesoRequest;
import org.uteq.backend.academico.estudiante.service.EstudianteService;

/**
 * CRUD completo de Estudiante con paginacion y soft delete.
 * Endpoints sensibles requieren rol ADMINISTRADOR via @PreAuthorize.
 */
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudiantePageResponse<EstudianteResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idEstudiante,asc") String[] sort) {
        
        String campo = sort[0];
        Sort.Direction dir = sort.length > 1 && "desc".equalsIgnoreCase(sort[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(dir, campo));
        
        return ResponseEntity.ok(estudianteService.listar(pageRequest));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> crear(
            @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estudianteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.ok(estudianteService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Ajustado a Long idCategoria para coincidir con la relación BD/Service
    @GetMapping("/conteo/categoria/{idCategoria}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Long> contarActivos(@PathVariable Long idCategoria) {
        return ResponseEntity.ok(estudianteService.contarActivosPorCategoria(idCategoria));
    }

    @PostMapping("/operaciones/desactivar-categoria")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desactivarPorCategoria(@RequestBody Long idCategoria) {
        estudianteService.desactivarPorCategoria(idCategoria);
        return ResponseEntity.ok().build();
    }

    /** Propone el siguiente codigo_estudiante del anio (no lo reserva: el alta sigue validando unicidad). */
    @GetMapping("/operaciones/siguiente-codigo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<String> siguienteCodigo(@RequestParam int anio) {
        return ResponseEntity.ok(estudianteService.generarSiguienteCodigo(anio));
    }

    /** Contacto rapido del representante del estudiante, para un entrenador ante una emergencia/lesion. */
    @GetMapping("/{id}/contacto-emergencia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<String> contactoEmergencia(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.contactoDeEmergencia(id));
    }

    /**
     * Habilita el acceso propio de un estudiante que ya existe (rol
     * ESTUDIANTE), para que pueda marcar su propia asistencia por QR.
     * Distinto de {@code crear}: no crea una Persona nueva, usa la que el
     * estudiante ya tiene. RECEPCIONISTA la necesita desde la pantalla
     * unificada de Personas (solo puede habilitar acceso de estudiantes,
     * no crear cuentas de otros roles).
     */
    @PostMapping("/{id}/acceso")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> habilitarAcceso(
            @PathVariable Long id, @Valid @RequestBody HabilitarAccesoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estudianteService.habilitarAcceso(id, request));
    }
}
