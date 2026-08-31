package org.uteq.backend.deportivo.posicion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "posiciones", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Posicion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_posicion")
    private Long idPosicion;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 5)
    private String abreviatura;

    @Column(length = 255)
    private String descripcion;

    @Column
    @Builder.Default
    private Boolean activo = true;
}
