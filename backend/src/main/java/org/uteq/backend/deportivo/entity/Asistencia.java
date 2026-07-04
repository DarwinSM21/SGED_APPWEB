package org.uteq.backend.deportivo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.estudiante.entity.Estudiante;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "asistencias", schema = "deportivo",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_asistencia_sesion_estudiante", 
                             columnNames = {"id_sesion", "id_estudiante"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asistencia")
    private Long idAsistencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sesion", nullable = false)
    private SesionEntrenamiento sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "hora_entrada")
    private LocalTime horaEntrada;

    @Column(name = "metodo", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private MetodoAsistencia metodo = MetodoAsistencia.MANUAL;

    @Column(name = "estado", length = 15, nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoAsistencia estado;

    @Column(name = "observacion", length = 255)
    private String observacion;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = Instant.now();
    }

    public enum EstadoAsistencia {
        PRESENTE, TARDE, AUSENTE, JUSTIFICADO
    }

    public enum MetodoAsistencia {
        RFID, MANUAL
    }
}
