package org.uteq.backend.deportivo.entrenador.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.deportivo.especialidad.entity.Especialidad;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.OffsetDateTime;

@Entity
@Table(name = "entrenadores", schema = "deportivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrenador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrenador")
    private Long idEntrenador;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;

    @Column(name = "experiencia_anios")
    private Short experienciaAnios;

    @Column(name = "certificacion", length = 255)
    private String certificacion;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.activo == null) this.activo = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}