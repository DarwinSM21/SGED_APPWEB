package org.uteq.backend.deportivo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.dto.EntrenadorRequest;
import org.uteq.backend.deportivo.dto.EntrenadorResponse;
import org.uteq.backend.deportivo.service.EntrenadorService;

import java.util.List;

@RestController
@RequestMapping("/api/entrenadores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Entrenadores", description = "Gestión de entrenadores")
@SecurityRequirement(name = "bearer-jwt")
public class EntrenadorController {

    private final EntrenadorService entrenadorService;

    /**
     * Crear nuevo entrenador
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear entrenador",
               description = "Crea un nuevo registro de entrenador (requiere rol ADMINISTRADOR)")
    public ResponseEntity<EntrenadorResponse> crearEntrenador(
            @RequestBody EntrenadorRequest request) {
        
        log.info("Creando entrenador: Persona={}", request.getIdPersona());
        EntrenadorResponse response = entrenadorService.crearEntrenador(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener entrenador por ID
     */
    @GetMapping("/{idEntrenador}")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Obtener entrenador",
               description = "Obtiene los detalles de un entrenador")
    public ResponseEntity<EntrenadorResponse> obtenerEntrenador(
            @PathVariable Long idEntrenador) {
        
        log.info("Obteniendo entrenador: {}", idEntrenador);
        EntrenadorResponse response = entrenadorService.obtenerEntrenador(idEntrenador);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener todos los entrenadores activos (paginado)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Listar entrenadores",
               description = "Obtiene una lista paginada de entrenadores activos")
    public ResponseEntity<Page<EntrenadorResponse>> listarEntrenadores(
            @Parameter(description = "Número de página (0-indexed), tamaño, ordenamiento")
            Pageable pageable) {
        
        log.info("Listando entrenadores");
        Page<EntrenadorResponse> response = entrenadorService.listarEntrenadores(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener todos los entrenadores activos (sin paginar)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Obtener todos los entrenadores",
               description = "Obtiene una lista completa de entrenadores activos (sin paginación)")
    public ResponseEntity<List<EntrenadorResponse>> obtenerTodosEntrenadores() {
        
        log.info("Obteniendo todos los entrenadores");
        List<EntrenadorResponse> response = entrenadorService.obtenerTodosEntrenadores();
        return ResponseEntity.ok(response);
    }

    /**
     * Actualizar entrenador
     */
    @PutMapping("/{idEntrenador}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar entrenador",
               description = "Actualiza los datos de un entrenador (requiere rol ADMINISTRADOR)")
    public ResponseEntity<EntrenadorResponse> actualizarEntrenador(
            @PathVariable Long idEntrenador,
            @RequestBody EntrenadorRequest request) {
        
        log.info("Actualizando entrenador: {}", idEntrenador);
        EntrenadorResponse response = entrenadorService.actualizarEntrenador(idEntrenador, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Desactivar entrenador (soft delete)
     */
    @DeleteMapping("/{idEntrenador}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desactivar entrenador",
               description = "Desactiva un entrenador (soft delete, requiere rol ADMINISTRADOR)")
    public ResponseEntity<Void> desactivarEntrenador(
            @PathVariable Long idEntrenador) {
        
        log.info("Desactivando entrenador: {}", idEntrenador);
        entrenadorService.desactivarEntrenador(idEntrenador);
        return ResponseEntity.noContent().build();
    }
}
