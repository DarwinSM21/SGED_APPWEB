package org.uteq.backend.inventario.movimiento.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.Instant;

@Entity
@Table(name = "movimientos_stock", schema = "inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoStock {
    public enum TipoMovimiento { ENTRADA, SALIDA, AJUSTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_articulo", nullable = false)
    private Articulo articulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 10)
    private TipoMovimiento tipoMovimiento;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 255)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id_usuario", nullable = false)
    private Usuario registradoPor;

    @Column(name = "fecha_movimiento", nullable = false)
    @Builder.Default
    private Instant fechaMovimiento = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
