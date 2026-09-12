package org.uteq.backend.deportivo.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.injury.entity.Injury;
import org.uteq.backend.deportivo.position.entity.Position;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluacion_estudiante", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion_estudiante")
    private Long idEvaluacionEstudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private DailyEvaluation evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Student estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria_dia", nullable = false)
    private Category categoriaDia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_posicion_jugada")
    private Position posicionJugada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lesion")
    private Injury lesion;

    @OneToMany(mappedBy = "evaluacionEstudiante", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationDetail> detalles = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;
}
