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

public final class PaymentDtos {
    private PaymentDtos() {}

    public record RegisterMembershipRequest(
            @NotNull Long studentId,
            @NotNull @Min(2020) @Max(2100) Integer year,
            @NotEmpty List<@Min(1) @Max(12) Integer> months,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            LocalDate paymentDate
    ) {}

    public record RegisterDailyRequest(
            @NotNull Long studentId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            LocalDate paymentDate
    ) {}

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

    public record CancelPaymentRequest(
            @NotBlank(message = "Indica por qué se anula el pago")
            @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
            String reason
    ) {}

    public record MonthlyIncomeResponse(
            Integer year,
            Integer month,
            BigDecimal total,
            Long paymentCount
    ) {}

    public record IncomeHistoryResponse(
            List<MonthlyIncomeResponse> months,
            BigDecimal total,
            BigDecimal monthlyAverage,
            MonthlyIncomeResponse bestMonth
    ) {}
}
