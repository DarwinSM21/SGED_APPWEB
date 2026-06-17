package org.uteq.backend.auth.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long idRol;

    private String nombre;

    private String descripcion;
}