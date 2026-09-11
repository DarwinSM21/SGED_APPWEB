package org.uteq.backend.academico.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.payment.dto.PaymentDtos.*;
import org.uteq.backend.academico.payment.entity.Payment;
import org.uteq.backend.academico.payment.service.PaymentService;

import java.util.List;

/**
 * Pagos. Los registra recepción (o un administrador); el usuario que
 * registra se resuelve del contexto de seguridad, nunca de un id del
 * cliente.
 *
 * <p>Los métodos llevan {@code @Transactional} propio porque
 * {@code toResponse()} navega relaciones LAZY ({@code Payment -> Student ->
 * Person}, {@code Payment -> UserAccount -> Person}) con open-in-view
 * deshabilitado.
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService pagoService;

    /**
     * Registra el pago de una o varias mensualidades de membresía (todo o
     * nada: si un mes ya está cubierto, no se cobra ninguno).
     *
     * @param request estudiante, año, meses, monto y fecha; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con la lista de pagos creados
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante no existe ({@code 404})
     * @throws IllegalArgumentException si algún mes ya está cubierto
     *         ({@code 422})
     */
    @PostMapping("/membresia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<List<PaymentResponse>> registerMembership(@Valid @RequestBody RegisterMembershipRequest request) {
        var pagos = pagoService.registerMembership(
                request.idEstudiante(), request.anio(), request.meses(),
                request.monto(), request.fechaPago(), authenticatedUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagos.stream().map(this::toResponse).toList());
    }

    /**
     * Registra un pago diario (no cubre período).
     *
     * @param request estudiante, monto y fecha; validado con {@code @Valid}
     * @return {@code 201 Created} con el pago creado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante no existe ({@code 404})
     */
    @PostMapping("/diario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<PaymentResponse> registerDaily(@Valid @RequestBody RegisterDailyRequest request) {
        var pago = pagoService.registerDaily(
                request.idEstudiante(), request.monto(), request.fechaPago(), authenticatedUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(pago));
    }

    /**
     * Anula un pago mal registrado (no lo borra: queda con quién, cuándo y
     * por qué se anuló).
     *
     * @param idPago  identificador del pago
     * @param request motivo de la anulación; validado con {@code @Valid}
     * @return {@code 200 OK} con el pago anulado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el pago no existe ({@code 404})
     * @throws IllegalArgumentException si el pago ya estaba anulado
     *         ({@code 422})
     */
    @PostMapping("/{idPago}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<PaymentResponse> cancel(@PathVariable Long idPago,
                                               @Valid @RequestBody CancelPaymentRequest request) {
        return ResponseEntity.ok(toResponse(
                pagoService.cancel(idPago, request.motivo(), authenticatedUsername())));
    }

    /**
     * Historial de pagos de un estudiante, del más reciente al más antiguo.
     *
     * @param idEstudiante identificador del estudiante
     * @return {@code 200 OK} con la lista de pagos
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante no existe ({@code 404})
     */
    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PaymentResponse>> history(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(pagoService.historyFor(idEstudiante).stream().map(this::toResponse).toList());
    }

    /**
     * Total ingresado en el mes calendario en curso.
     *
     * @return {@code 200 OK} con el total y el número de pagos del mes
     */
    @GetMapping("/ingresos-mes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<MonthlyIncomeResponse> currentMonthIncome() {
        return ResponseEntity.ok(pagoService.currentMonthIncome());
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
    public ResponseEntity<IncomeHistoryResponse> incomeHistory(
            @RequestParam(defaultValue = "6") int meses) {
        return ResponseEntity.ok(pagoService.incomeHistory(meses));
    }

    private String authenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private PaymentResponse toResponse(Payment p) {
        var persona = p.getStudent().getPerson();
        var registrador = p.getRegisteredBy().getPerson();
        return new PaymentResponse(
                p.getId(),
                p.getStudent().getId(),
                persona.getName() + " " + persona.getLastName(),
                p.getType(),
                p.getYear() != null ? p.getYear().intValue() : null,
                p.getMonth() != null ? p.getMonth().intValue() : null,
                p.getAmount(),
                p.getPaymentDate(),
                registrador.getName() + " " + registrador.getLastName(),
                p.getCanceledAt(),
                p.getCanceledBy() == null ? null
                        : p.getCanceledBy().getPerson().getName() + " "
                          + p.getCanceledBy().getPerson().getLastName(),
                p.getCancellationReason());
    }
}
