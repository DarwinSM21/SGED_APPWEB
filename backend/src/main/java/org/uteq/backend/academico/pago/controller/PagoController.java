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
 * Pagos. Los registra recepción (o un administrador); el usuario que
 * registra se resuelve del contexto de seguridad, nunca de un id del
 * cliente.
 *
 * <p>Los métodos llevan {@code @Transactional} propio porque
 * {@code aResponse()} navega relaciones LAZY ({@code Pago -> Estudiante ->
 * Persona}, {@code Pago -> Usuario -> Persona}) con open-in-view
 * deshabilitado.
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {
    private final PagoService pagoService;

    /**
     * Registra el pago de una o varias mensualidades de membresía (todo o
     * nada: si un mes ya está cubierto, no se cobra ninguno).
     *
     * @param request estudiante, año, meses, monto y fecha; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con la lista de pagos creados
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante no existe ({@code 404})
     * @throws IllegalArgumentException si algún mes ya está cubierto
     *         ({@code 422})
     */
    @PostMapping("/membresia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<List<PagoResponse>> registrarMembresia(@Valid @RequestBody RegistrarMembresiaRequest request) {
        var pagos = pagoService.registrarMembresia(
                request.idEstudiante(), request.anio(), request.meses(),
                request.monto(), request.fechaPago(), usernameAutenticado());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagos.stream().map(this::aResponse).toList());
    }

    /**
     * Registra un pago diario (no cubre período).
     *
     * @param request estudiante, monto y fecha; validado con {@code @Valid}
     * @return {@code 201 Created} con el pago creado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante no existe ({@code 404})
     */
    @PostMapping("/diario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<PagoResponse> registrarDiario(@Valid @RequestBody RegistrarDiarioRequest request) {
        var pago = pagoService.registrarDiario(
                request.idEstudiante(), request.monto(), request.fechaPago(), usernameAutenticado());
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(pago));
    }

    /**
     * Anula un pago mal registrado (no lo borra: queda con quién, cuándo y
     * por qué se anuló).
     *
     * @param idPago  identificador del pago
     * @param request motivo de la anulación; validado con {@code @Valid}
     * @return {@code 200 OK} con el pago anulado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el pago no existe ({@code 404})
     * @throws IllegalArgumentException si el pago ya estaba anulado
     *         ({@code 422})
     */
    @PostMapping("/{idPago}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<PagoResponse> anular(@PathVariable Long idPago,
                                               @Valid @RequestBody AnularPagoRequest request) {
        return ResponseEntity.ok(aResponse(
                pagoService.anular(idPago, request.motivo(), usernameAutenticado())));
    }

    /**
     * Historial de pagos de un estudiante, del más reciente al más antiguo.
     *
     * @param idEstudiante identificador del estudiante
     * @return {@code 200 OK} con la lista de pagos
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante no existe ({@code 404})
     */
    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PagoResponse>> historial(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(pagoService.historialDe(idEstudiante).stream().map(this::aResponse).toList());
    }

    /**
     * Total ingresado en el mes calendario en curso.
     *
     * @return {@code 200 OK} con el total y el número de pagos del mes
     */
    @GetMapping("/ingresos-mes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<IngresosMesResponse> ingresosDelMes() {
        return ResponseEntity.ok(pagoService.ingresosDelMes());
    }

    /**
     * Serie de recaudación de los últimos meses, contando el actual.
     *
     * @param meses número de meses a incluir (se acota internamente a
     *              {@code [1, 24]}); por defecto 6
     * @return {@code 200 OK} con la serie, total, promedio y mejor mes
     */
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
