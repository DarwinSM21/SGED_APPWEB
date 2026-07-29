package org.uteq.backend.seguridad.estado.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estados_general", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoGeneral {

    @Id
    @Column(name = "id_estado_general")
    private Long idEstadoGeneral;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;
}