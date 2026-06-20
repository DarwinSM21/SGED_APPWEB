package org.uteq.backend.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;


@Entity
@Table(name = "usuarios", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona")
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_general")
    private EstadoGeneral estadoGeneral;

    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    private Boolean activo;

    @OneToMany(mappedBy = "usuario")
    private List<UsuarioRol> usuarioRoles;
}
