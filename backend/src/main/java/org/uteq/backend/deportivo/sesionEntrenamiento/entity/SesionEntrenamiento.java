package org.uteq.backend.deportivo.sesionEntrenamiento.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.deportivo.entrenadorCategoria.entity.EntrenadorCategoria;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sesion_entrenamiento", schema = "deportivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion_entrenamiento")
    private Long idSesionEntrenamiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrenador_categoria", nullable = false)
    private EntrenadorCategoria entrenadorCategoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_general")
    private EstadoGeneral estadoGeneral;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
