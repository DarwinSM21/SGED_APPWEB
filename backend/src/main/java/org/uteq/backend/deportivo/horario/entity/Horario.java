package org.uteq.backend.deportivo.horario.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Horario fijo semanal de un entrenador: "SUB-12 entrena Lunes y Miercoles,
 * 16:00-18:00". De aqui se generan automaticamente las filas concretas de
 * sesiones_entrenamiento el dia que corresponde (ver
 * HorarioService.generarSesionesDeHoy()); esa sesion generada queda
 * enlazada de vuelta via SesionEntrenamiento.horario.
 */
@Entity
@Table(name = "horarios_entrenamiento", schema = "deportivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long idHorario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrenador", nullable = false)
    private Entrenador entrenador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    /**
     * 1=Lunes ... 7=Domingo, igual que LocalDate.getDayOfWeek().getValue().
     * Short y no Integer: la columna es SMALLINT, y ddl-auto: validate exige
     * el tipo exacto (no basta con que quepa).
     */
    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(length = 100)
    private String campo;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;
}
