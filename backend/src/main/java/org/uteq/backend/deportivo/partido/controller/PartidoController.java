package org.uteq.backend.deportivo.partido.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.partido.dto.AlineacionDtos.GuardarAlineacionRequest;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.AlineacionResponse;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.FeedbackAlineacionResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.CrearPartidoRequest;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoPageResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.ResultadoRequest;
import org.uteq.backend.deportivo.partido.service.AlineacionService;
import org.uteq.backend.deportivo.partido.service.PartidoService;

@RestController
@RequestMapping("/api/partidos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
public class PartidoController {
    private final PartidoService partidoService;
    private final AlineacionService alineacionService;

    @GetMapping
    public ResponseEntity<PartidoPageResponse> listar(
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(partidoService.listar(idCategoria, page, size));
    }

    @GetMapping("/{idPartido}")
    public ResponseEntity<PartidoResponse> ver(@PathVariable Long idPartido) {
        return ResponseEntity.ok(partidoService.buscarPorId(idPartido));
    }

    @PostMapping
    public ResponseEntity<PartidoResponse> crear(@Valid @RequestBody CrearPartidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partidoService.crear(request));
    }

    @PutMapping("/{idPartido}/resultado")
    public ResponseEntity<PartidoResponse> registrarResultado(
            @PathVariable Long idPartido, @Valid @RequestBody ResultadoRequest request) {
        return ResponseEntity.ok(partidoService.registrarResultado(idPartido, request));
    }

    @PostMapping("/{idPartido}/reapertura")
    public ResponseEntity<PartidoResponse> reabrir(@PathVariable Long idPartido) {
        return ResponseEntity.ok(partidoService.reabrir(idPartido));
    }

    @DeleteMapping("/{idPartido}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idPartido) {
        partidoService.eliminar(idPartido);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idPartido}/alineacion")
    @Transactional(readOnly = true)
    public ResponseEntity<AlineacionResponse> verAlineacion(@PathVariable Long idPartido) {
        return ResponseEntity.ok(alineacionService.ver(idPartido));
    }

    @PutMapping("/{idPartido}/alineacion")
    @Transactional
    public ResponseEntity<AlineacionResponse> guardarAlineacion(
            @PathVariable Long idPartido, @Valid @RequestBody GuardarAlineacionRequest request) {
        return ResponseEntity.ok(alineacionService.guardar(idPartido, request));
    }

    @DeleteMapping("/{idPartido}/alineacion")
    @Transactional
    public ResponseEntity<AlineacionResponse> restablecerAlineacion(@PathVariable Long idPartido) {
        return ResponseEntity.ok(alineacionService.restablecer(idPartido));
    }

    @PostMapping("/{idPartido}/alineacion/feedback")
    @Transactional(readOnly = true)
    public ResponseEntity<FeedbackAlineacionResponse> feedback(@PathVariable Long idPartido) {
        return ResponseEntity.ok(alineacionService.feedback(idPartido));
    }
}
