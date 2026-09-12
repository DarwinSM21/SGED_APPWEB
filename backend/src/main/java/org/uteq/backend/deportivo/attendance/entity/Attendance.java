package org.uteq.backend.deportivo.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.deportivo.session.entity.TrainingSession;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "asistencias", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {
    public static final String METODO_QR = "QR";
    public static final String METODO_MANUAL = "MANUAL";
    public static final String ESTADO_PRESENTE = "PRESENTE";
    public static final String ESTADO_TARDE = "TARDE";
    public static final String ESTADO_AUSENTE = "AUSENTE";
    public static final String ESTADO_JUSTIFICADO = "JUSTIFICADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asistencia")
    private Long idAsistencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sesion", nullable = false)
    private TrainingSession sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Student estudiante;

    @Column(name = "hora_entrada")
    private LocalTime horaEntrada;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String metodo = METODO_MANUAL;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(length = 255)
    private String observacion;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    /**
     * Indica si este registro de asistencia habilita al estudiante para
     * recibir una evaluación diaria de esa sesión.
     *
     * @return {@code true} si el estado es "presente" o "tarde"; {@code false}
     *         si faltó (una ausencia no genera evaluación)
     */
    @Transient
    public boolean enablesEvaluation() {
        return ESTADO_PRESENTE.equals(estado) || ESTADO_TARDE.equals(estado);
    }
}
