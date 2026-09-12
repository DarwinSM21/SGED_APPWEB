package org.uteq.backend.deportivo.match.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.deportivo.category.entity.Category;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "partidos", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partido")
    private Long idPartido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Category categoria;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column
    private LocalTime hora;

    @Column(name = "goles_favor")
    private Short golesFavor;

    @Column(name = "goles_contra")
    private Short golesContra;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false)
    private Boolean cerrado;

    @Column(name = "cerrado_en")
    private Instant cerradoEn;

    @Column(name = "cerrado_por_id_usuario")
    private Long cerradoPorIdUsuario;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    /**
     * Indica si ya se registró el marcador del partido.
     *
     * @return {@code true} si tanto los goles a favor como en contra tienen
     *         un valor cargado
     */
    @Transient
    public boolean hasResult() {
        return golesFavor != null && golesContra != null;
    }

    /**
     * Indica si el partido fue cerrado por un entrenador o administrador.
     *
     * @return {@code true} si el partido está cerrado; un partido cerrado no
     *         admite más cambios de resultado ni de alineación
     */
    @Transient
    public boolean isClosed() {
        return Boolean.TRUE.equals(cerrado);
    }
}
