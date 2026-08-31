package org.uteq.backend.deportivo.evaluacion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluaciones_diarias", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvaluacionDiaria {
    public static final String BORRADOR = "BORRADOR";
    public static final String FINALIZADA = "FINALIZADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sesion", nullable = false, unique = true)
    private SesionEntrenamiento sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrenador", nullable = false)
    private Entrenador entrenador;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "observacion_general", columnDefinition = "text")
    private String observacionGeneral;

    @Column(nullable = false, length = 15)
    @Builder.Default
    private String estado = BORRADOR;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluacionEstudiante> jugadores = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @Transient
    public boolean estaFinalizada() {
        return FINALIZADA.equals(estado);
    }
}
