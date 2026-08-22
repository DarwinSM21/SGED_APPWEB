package org.uteq.backend.academico.pago.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Pago de un estudiante. MEMBRESIA cubre un mes calendario exacto
 * (anio+mes obligatorios, sin poder repetirse: ver el indice unico
 * parcial de db/schema.sql); DIARIO es puntual y no lleva periodo.
 */
@Entity
@Table(name = "pagos", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    public enum TipoPago { MEMBRESIA, DIARIO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPago tipo;

    private Short anio;

    private Short mes;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id_usuario", nullable = false)
    private Usuario registradoPor;

    // Anulacion. Las tres viajan juntas o ninguna: la base lo exige con
    // chk_pago_anulacion_completa, porque un pago anulado sin motivo no le
    // explica nada a quien revise las cuentas despues.
    @Column(name = "anulado_en")
    private java.time.OffsetDateTime anuladoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por_id_usuario")
    private Usuario anuladoPor;

    @Column(name = "motivo_anulacion", length = 255)
    private String motivoAnulacion;

    /** Un pago cuenta para los totales solo mientras no este anulado. */
    public boolean estaVigente() {
        return anuladoEn == null;
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
