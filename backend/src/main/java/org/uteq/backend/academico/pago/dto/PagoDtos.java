package org.uteq.backend.academico.pago.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PagoDtos {

    private PagoDtos() {}

    public record RegistrarMembresiaRequest(
            @NotNull Long idEstudiante,
            @NotNull @Min(2020) @Max(2100) Integer anio,
            @NotEmpty List<@Min(1) @Max(12) Integer> meses,
            @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
            LocalDate fechaPago
    ) {}

    public record RegistrarDiarioRequest(
            @NotNull Long idEstudiante,
            @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
            LocalDate fechaPago
    ) {}

    public record PagoResponse(
            Long idPago,
            Long idEstudiante,
            String estudiante,
            TipoPago tipo,
            Integer anio,
            Integer mes,
            BigDecimal monto,
            LocalDate fechaPago,
            String registradoPor,
            /** Null mientras el pago siga vigente. */
            java.time.OffsetDateTime anuladoEn,
            String anuladoPor,
            String motivoAnulacion
    ) {
        public boolean vigente() {
            return anuladoEn == null;
        }
    }

    /**
     * El motivo es obligatorio a proposito: anular sin decir por que deja el
     * historial con un hueco que nadie sabra explicar dentro de tres meses.
     */
    public record AnularPagoRequest(
            @NotBlank(message = "Indica por qué se anula el pago")
            @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
            String motivo
    ) {}

    public record IngresosMesResponse(
            Integer anio,
            Integer mes,
            BigDecimal total,
            Long cantidadPagos
    ) {}

    /**
     * Serie para el grafico de barras del tablero: un punto por mes, en
     * orden cronologico y sin huecos -un mes sin cobros viaja en cero, no se
     * omite: si faltara, el grafico dibujaria dos meses contiguos que en
     * realidad estan separados y la lectura de la tendencia seria falsa-.
     */
    public record HistoricoIngresosResponse(
            List<IngresosMesResponse> meses,
            BigDecimal total,
            BigDecimal promedioMensual,
            /** Mes con mayor recaudacion del rango; null si no hubo ningun cobro. */
            IngresosMesResponse mejorMes
    ) {}
}
