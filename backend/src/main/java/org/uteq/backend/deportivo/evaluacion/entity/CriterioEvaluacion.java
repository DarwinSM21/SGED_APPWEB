package org.uteq.backend.deportivo.evaluacion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Criterio configurable sobre el que se califica (Tecnica, Condicion fisica,
 * Tactica, Actitud). Son datos sembrados, no fijos en codigo: la escuela puede
 * agregar o desactivar criterios sin recompilar.
 */
@Entity
@Table(name = "criterios_evaluacion", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CriterioEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    private Long idCriterio;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "puntaje_maximo", nullable = false)
    @Builder.Default
    private Short puntajeMaximo = 10;

    @Column
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;
}
