package org.uteq.backend.academico.pago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.pago.dto.PagoDtos.*;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.service.PagoService;

import java.util.List;

/**
 * Pagos. Los registra recepcion (o un administrador); el principal
 * autenticado se resuelve aqui, nunca se confia en un id de cliente,
 * mismo criterio ya usado para el resto de altas de este modulo.
 *
 * <p>Los metodos llevan @Transactional propio porque aResponse() navega
 * Pago -&gt; Estudiante -&gt; Persona y Pago -&gt; Usuario -&gt; Persona (relaciones
 * LAZY) con open-in-view deshabilitado.
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/membresia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<List<PagoResponse>> registrarMembresia(@Valid @RequestBody RegistrarMembresiaRequest request) {
        var pagos = pagoService.registrarMembresia(
                request.idEstudiante(), request.anio(), request.meses(),
                request.monto(), request.fechaPago(), usernameAutenticado());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagos.stream().map(this::aResponse).toList());
    }

    @PostMapping("/diario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<PagoResponse> registrarDiario(@Valid @RequestBody RegistrarDiarioRequest request) {
        var pago = pagoService.registrarDiario(
                request.idEstudiante(), request.monto(), request.fechaPago(), usernameAutenticado());
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(pago));
    }

    @PostMapping("/{idPago}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<PagoResponse> anular(@PathVariable Long idPago,
                                               @Valid @RequestBody AnularPagoRequest request) {
        return ResponseEntity.ok(aResponse(
                pagoService.anular(idPago, request.motivo(), usernameAutenticado())));
    }

    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PagoResponse>> historial(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(pagoService.historialDe(idEstudiante).stream().map(this::aResponse).toList());
    }

    @GetMapping("/ingresos-mes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<IngresosMesResponse> ingresosDelMes() {
        return ResponseEntity.ok(pagoService.ingresosDelMes());
    }

    @GetMapping("/ingresos-historico")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<HistoricoIngresosResponse> historicoIngresos(
            @RequestParam(defaultValue = "6") int meses) {
        return ResponseEntity.ok(pagoService.historicoIngresos(meses));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private PagoResponse aResponse(Pago p) {
        var persona = p.getEstudiante().getPersona();
        var registrador = p.getRegistradoPor().getPersona();
        return new PagoResponse(
                p.getIdPago(),
                p.getEstudiante().getIdEstudiante(),
                persona.getNombre() + " " + persona.getApellido(),
                p.getTipo(),
                p.getAnio() != null ? p.getAnio().intValue() : null,
                p.getMes() != null ? p.getMes().intValue() : null,
                p.getMonto(),
                p.getFechaPago(),
                registrador.getNombre() + " " + registrador.getApellido(),
                p.getAnuladoEn(),
                p.getAnuladoPor() == null ? null
                        : p.getAnuladoPor().getPersona().getNombre() + " "
                          + p.getAnuladoPor().getPersona().getApellido(),
                p.getMotivoAnulacion());
    }
}
