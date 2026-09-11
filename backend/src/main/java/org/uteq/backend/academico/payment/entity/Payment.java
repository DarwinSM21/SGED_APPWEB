package org.uteq.backend.academico.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pagos", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    public enum PaymentType { MEMBRESIA, DIARIO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType type;

    private Short year;

    private Short month;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id_usuario", nullable = false)
    private UserAccount registeredBy;

    @Column(name = "anulado_en")
    private java.time.OffsetDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por_id_usuario")
    private UserAccount canceledBy;

    @Column(name = "motivo_anulacion", length = 255)
    private String cancellationReason;

    /**
     * @return {@code true} si el pago no fue anulado
     */
    public boolean isActive() {
        return canceledAt == null;
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
