package org.uteq.backend.estudiante.entity;
import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.auth.entity.Persona;

import java.time.Instant;

@Entity
@Table(name = "estudiantes", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante")
    private Long idEstudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona")
    private Persona persona;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "fecha_ingreso")
    private Instant fechaIngreso;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "creado_en")
    private Instant creadoEn;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
        this.activo = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = Instant.now();
    }
}