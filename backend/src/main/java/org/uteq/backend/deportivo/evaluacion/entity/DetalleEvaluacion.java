package org.uteq.backend.deportivo.evaluacion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Puntaje de un criterio para un jugador en una evaluacion. Desde V7 cuelga de
 * EvaluacionEstudiante y no guarda ni el estudiante ni la posicion: esos datos
 * viven una sola vez, un nivel arriba.
 */
@Entity
@Table(name = "detalle_evaluacion", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evaluacion_estudiante", nullable = false)
    private EvaluacionEstudiante evaluacionEstudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_criterio", nullable = false)
    private CriterioEvaluacion criterio;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal puntaje;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;
}
