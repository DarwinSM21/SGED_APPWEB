package org.uteq.backend.deportivo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "evaluaciones_diarias", schema = "deportivo",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_evaluacion_sesion", 
                             columnNames = "id_sesion")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sesion", nullable = false)
    private SesionEntrenamiento sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrenador", nullable = false)
    private Entrenador entrenador;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "observacion_general", columnDefinition = "TEXT")
    private String observacionGeneral;

    @Column(name = "estado", length = 15, nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoEvaluacion estado = EstadoEvaluacion.BORRADOR;

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

    public enum EstadoEvaluacion {
        BORRADOR, FINALIZADA
    }
}
