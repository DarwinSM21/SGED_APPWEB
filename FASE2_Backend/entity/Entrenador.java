package org.uteq.backend.deportivo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.auth.entity.Persona;

import java.time.Instant;
import java.time.LocalDate;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    @Column(name = "especialidad", length = 100)
    private String especialidad;

    @Column(name = "fecha_contratacion")
    private LocalDate fechaContratacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
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
