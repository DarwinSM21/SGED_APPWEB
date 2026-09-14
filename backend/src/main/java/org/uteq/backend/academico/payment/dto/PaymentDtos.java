package org.uteq.backend.academico.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.academico.payment.entity.Payment.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Contenedor de los DTO de pagos y membresías. */
public final class PaymentDtos {
    private PaymentDtos() {}

    /**
     * Registro de un pago de membresía, que puede cubrir varios meses a la vez.
     *
     * @param studentId    identificador del estudiante que paga
     * @param year         año calendario que cubre el pago
     * @param months       meses del año cubiertos (1-12)
     * @param amount       monto total pagado
     * @param paymentDate  fecha del pago, o {@code null} para usar la fecha actual
     */
    public record RegisterMembershipRequest(
            @NotNull Long studentId,
            @NotNull @Min(2020) @Max(2100) Integer year,
            @NotEmpty List<@Min(1) @Max(12) Integer> months,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            LocalDate paymentDate
    ) {}

    /**
     * Registro de un pago diario/eventual, sin periodo de membresía asociado.
     *
     * @param studentId    identificador del estudiante que paga
     * @param amount       monto pagado
     * @param paymentDate  fecha del pago, o {@code null} para usar la fecha actual
     */
    public record RegisterDailyRequest(
            @NotNull Long studentId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            LocalDate paymentDate
    ) {}

    /**
     * Vista de un pago para el cliente.
     *
     * @param paymentId     identificador del pago
     * @param studentId     identificador del estudiante
     * @param student       nombre del estudiante
     * @param type          tipo de pago (membresía o diario)
     * @param year          año que cubre, si es de membresía
     * @param month         mes que cubre, si es de membresía
     * @param amount        monto pagado
     * @param paymentDate   fecha del pago
     * @param registeredBy  usuario que registró el pago
     * @param voidedAt      fecha y hora de anulación, o {@code null} si sigue vigente
     * @param voidedBy      usuario que anuló el pago, si aplica
     * @param voidReason    motivo de la anulación, si aplica
     */
    public record PaymentResponse(
            Long paymentId,
            Long studentId,
            String student,
            PaymentType type,
            Integer year,
            Integer month,
            BigDecimal amount,
            LocalDate paymentDate,
            String registeredBy,
            java.time.OffsetDateTime voidedAt,
            String voidedBy,
            String voidReason
    ) {
        /**
         * @return {@code true} si el pago no fue anulado
         */
        public boolean active() {
            return voidedAt == null;
        }
    }

    /**
     * Motivo de anulación de un pago.
     *
     * @param reason explicación de por qué se anula
     */
    public record CancelPaymentRequest(
            @NotBlank(message = "Indica por qué se anula el pago")
            @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
            String reason
    ) {}

    /**
     * Ingresos totales de un mes calendario.
     *
     * @param year          año
     * @param month         mes (1-12)
     * @param total         suma de montos pagados en el mes, sin anulados
     * @param paymentCount  cantidad de pagos vigentes en el mes
     */
    public record MonthlyIncomeResponse(
            Integer year,
            Integer month,
            BigDecimal total,
            Long paymentCount
    ) {}

    /**
     * Historial de ingresos mensuales de un rango de tiempo.
     *
     * @param months          ingresos mes a mes
     * @param total           suma de todos los meses del rango
     * @param monthlyAverage  promedio mensual del rango
     * @param bestMonth       el mes con mayor ingreso del rango, o {@code null} si no hay datos
     */
    public record IncomeHistoryResponse(
            List<MonthlyIncomeResponse> months,
            BigDecimal total,
            BigDecimal monthlyAverage,
            MonthlyIncomeResponse bestMonth
    ) {}
}
