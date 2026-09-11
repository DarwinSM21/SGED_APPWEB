package org.uteq.backend.inventario.item.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "articulos", schema = "inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    public enum ItemType { UNIFORME, BALON, IMPLEMENTO, OTRO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_articulo")
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType type;

    @Column(length = 20)
    private String size;

    @Column(length = 255)
    private String description;

    @Column(name = "stock_actual", nullable = false)
    @Builder.Default
    private Integer currentStock = 0;

    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer minimumStock = 0;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    @Builder.Default
    private String unitOfMeasure = "unidad";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
