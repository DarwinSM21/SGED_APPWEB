package org.uteq.backend.seguridad.auditoria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auditoria", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "fecha", nullable = false)
    private OffsetDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "usuario_nombre", nullable = false, length = 150)
    private String usuarioNombre;

    @Column(name = "rol", length = 50)
    private String rol;

    @Column(name = "accion", nullable = false, length = 30)
    private String accion;

    @Column(name = "entidad", length = 100)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "descripcion", nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "ip", length = 45)
    private String ip;

    @PrePersist
    protected void onCreate() {
        if (this.fecha == null) {
            this.fecha = OffsetDateTime.now();
        }
    }
}
