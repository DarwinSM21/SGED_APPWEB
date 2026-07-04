package org.uteq.backend.deportivo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "criterios_evaluacion", schema = "deportivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterioEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    private Long idCriterio;

    @Column(name = "nombre", length = 100, nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "puntaje_maximo", nullable = false)
    private Short puntajeMaximo = 10;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
        this.activo = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = Instant.now();
    }
}
