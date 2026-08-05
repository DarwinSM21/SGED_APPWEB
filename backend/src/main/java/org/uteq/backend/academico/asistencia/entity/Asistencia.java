package org.uteq.backend.academico.asistencia.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.estadoAsistencia.entity.EstadoAsistencia;
import org.uteq.backend.deportivo.sesionEntrenamiento.entity.SesionEntrenamiento;

import java.time.LocalDate;

@Entity
@Table(name = "asistencia", schema = "academico")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sesion_entrenamiento", nullable = false)
    private SesionEntrenamiento sesionEntrenamiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_asistencia", nullable = false)
    private EstadoAsistencia estadoAsistencia;

    @Column(name = "fecha_registro")
    @Builder.Default
    private LocalDate fechaRegistro = LocalDate.now();
}
