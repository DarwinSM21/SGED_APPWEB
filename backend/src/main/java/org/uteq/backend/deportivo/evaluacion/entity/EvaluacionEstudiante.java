package org.uteq.backend.deportivo.evaluacion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.posicion.entity.Posicion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluacion_estudiante", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvaluacionEstudiante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion_estudiante")
    private Long idEvaluacionEstudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private EvaluacionDiaria evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria_dia", nullable = false)
    private Categoria categoriaDia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_posicion_jugada")
    private Posicion posicionJugada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lesion")
    private Lesion lesion;

    @OneToMany(mappedBy = "evaluacionEstudiante", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleEvaluacion> detalles = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;
}
