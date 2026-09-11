package org.uteq.backend.inventario.movement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.uteq.backend.inventario.item.entity.Item;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.time.Instant;

@Entity
@Table(name = "movimientos_stock", schema = "inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {
    public enum MovementType { ENTRADA, SALIDA, AJUSTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_articulo", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 10)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id_usuario", nullable = false)
    private UserAccount registeredBy;

    @Column(name = "fecha_movimiento", nullable = false)
    @Builder.Default
    private Instant movementDate = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
