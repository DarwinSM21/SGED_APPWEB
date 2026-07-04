package org.uteq.backend.deportivo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.estudiante.entity.Estudiante;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "detalle_evaluacion", schema = "deportivo",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_detalle_eval_est_criterio",
                             columnNames = {"id_evaluacion", "id_estudiante", "id_criterio"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private EvaluacionDiaria evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_criterio", nullable = false)
    private CriterioEvaluacion criterio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_posicion_jugada")
    private Posicion posicionJugada;

    @Column(name = "puntaje", nullable = false, precision = 4, scale = 1)
    private BigDecimal puntaje;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = Instant.now();
    }
}
