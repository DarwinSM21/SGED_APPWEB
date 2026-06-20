package org.uteq.backend.auth.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estados_general", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstadoGeneral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_general")
    private Long idEstadoGeneral;

    private String nombre;
}
