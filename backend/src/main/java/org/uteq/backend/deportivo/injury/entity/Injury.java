package org.uteq.backend.deportivo.injury.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.deportivo.coach.entity.Coach;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "lesiones", schema = "deportivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Injury {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lesion")
    private Long idLesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Student estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrenador", nullable = false)
    private Coach entrenador;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "fecha_lesion", nullable = false)
    private LocalDate fechaLesion;

    @Column(name = "fecha_estimada_retorno")
    private LocalDate fechaEstimadaRetorno;

    @Column(name = "fecha_alta")
    private LocalDate fechaAlta;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    /**
     * Indica si la lesión sigue en curso.
     *
     * @return {@code true} si aún no se registró fecha de alta médica
     */
    @Transient
    public boolean isActive() {
        return fechaAlta == null;
    }
}
