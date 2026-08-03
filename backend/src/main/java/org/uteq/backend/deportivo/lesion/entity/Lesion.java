package org.uteq.backend.deportivo.lesion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lesion de un estudiante, registrada por el entrenador como parte del
 * modulo de evaluacion diaria.
 *
 * Una lesion esta activa mientras {@code fechaAlta} sea null. Ese estado es
 * el que excluye al jugador de las sugerencias de plantilla y el que
 * distingue una ausencia por lesion de una falta sin motivo.
 */
@Entity
@Table(name = "lesiones", schema = "deportivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lesion")
    private Long idLesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    /** Entrenador que registro la lesion. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrenador", nullable = false)
    private Entrenador entrenador;

    /**
     * Texto libre sobre la condicion fisica de un menor de edad. Le aplican
     * los hallazgos H-02 (texto libre sin control de contenido) y H-06 (dato
     * de salud sin base legal documentada) de docs/etica/ETHICS.md.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "fecha_lesion", nullable = false)
    private LocalDate fechaLesion;

    @Column(name = "fecha_estimada_retorno")
    private LocalDate fechaEstimadaRetorno;

    /** Null mientras la lesion sigue activa. */
    @Column(name = "fecha_alta")
    private LocalDate fechaAlta;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @Transient
    public boolean estaActiva() {
        return fechaAlta == null;
    }
}
