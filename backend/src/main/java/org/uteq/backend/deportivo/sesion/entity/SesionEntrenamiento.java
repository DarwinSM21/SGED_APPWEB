package org.uteq.backend.deportivo.sesion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.horario.entity.Horario;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Instancia concreta de un entrenamiento en una fecha. Es la unidad a la que
 * se asocian tanto la asistencia como la evaluacion diaria: sin sesion no hay
 * nada que calificar.
 */
@Entity
@Table(name = "sesiones_entrenamiento", schema = "deportivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long idSesion;

    /**
     * Horario fijo que origino esta sesion (null si es una jornada extra
     * creada a mano, fuera del patron recurrente). Ver
     * HorarioService.generarSesionesProgramadas().
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_horario")
    private Horario horario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrenador", nullable = false)
    private Entrenador entrenador;

    /**
     * Categoria convocada. Desde V7 es clave foranea al catalogo, no el texto
     * libre que era antes: permite comparar de forma fiable la categoria de la
     * sesion con la del estudiante.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(length = 100)
    private String campo;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PROGRAMADA";

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;
}
