package org.uteq.backend.academico.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.academico.payment.entity.Payment.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record RegisterMembershipRequest(
            @NotNull Long idEstudiante,
            @NotNull @Min(2020) @Max(2100) Integer anio,
            @NotEmpty List<@Min(1) @Max(12) Integer> meses,
            @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
            LocalDate fechaPago
    ) {}

    public record RegisterDailyRequest(
            @NotNull Long idEstudiante,
            @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
            LocalDate fechaPago
    ) {}

    public record PaymentResponse(
            Long idPago,
            Long idEstudiante,
            String estudiante,
            TipoPago tipo,
            Integer anio,
            Integer mes,
            BigDecimal monto,
            LocalDate fechaPago,
            String registradoPor,
            java.time.OffsetDateTime anuladoEn,
            String anuladoPor,
            String motivoAnulacion
    ) {
        /**
         * @return {@code true} si el pago no fue anulado
         */
        public boolean vigente() {
            return anuladoEn == null;
        }
    }

    public record CancelPaymentRequest(
            @NotBlank(message = "Indica por qué se anula el pago")
            @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
            String motivo
    ) {}

    public record MonthlyIncomeResponse(
            Integer anio,
            Integer mes,
            BigDecimal total,
            Long cantidadPagos
    ) {}

    public record IncomeHistoryResponse(
            List<MonthlyIncomeResponse> meses,
            BigDecimal total,
            BigDecimal promedioMensual,
            MonthlyIncomeResponse mejorMes
    ) {}
}
