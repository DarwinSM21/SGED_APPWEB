package org.uteq.backend.deportivo.estadoAsistencia.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estado_asistencia", schema = "deportivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_asistencia")
    private Long idEstadoAsistencia;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;
}
