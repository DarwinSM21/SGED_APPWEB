package org.uteq.backend.deportivo.asistencia.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Asistencia de un estudiante a una sesion. Es la precondicion de la
 * evaluacion diaria: sin PRESENTE o TARDE en la sesion, el entrenador no
 * puede calificar a ese jugador.
 *
 * <p>{@code metodo} deja auditado COMO se marco: QR escaneado por el propio
 * estudiante, RFID, o registro MANUAL de la recepcionista cuando el celular
 * no esta disponible.
 */
@Entity
@Table(name = "asistencias", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asistencia {

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
    private SesionEntrenamiento sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

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

    /** Solo quien estuvo en el entrenamiento puede ser calificado. */
    @Transient
    public boolean habilitaEvaluacion() {
        return ESTADO_PRESENTE.equals(estado) || ESTADO_TARDE.equals(estado);
    }
}
