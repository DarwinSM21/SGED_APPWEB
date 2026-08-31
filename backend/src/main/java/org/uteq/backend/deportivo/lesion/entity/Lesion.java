package org.uteq.backend.deportivo.lesion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;

import java.time.Instant;
import java.time.LocalDate;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrenador", nullable = false)
    private Entrenador entrenador;

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

    @Transient
    public boolean estaActiva() {
        return fechaAlta == null;
    }
}
