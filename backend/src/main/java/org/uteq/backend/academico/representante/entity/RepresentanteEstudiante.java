package org.uteq.backend.academico.representante.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.estudiante.entity.Estudiante;

import java.time.Instant;

/**
 * Vinculo representante-estudiante como entidad de primera clase, no una
 * tabla puente plana: necesita su propio {@code activo} para que un
 * administrador pueda cortar el acceso de un tutor puntual (p.ej. disputa
 * de custodia) sin tocar su cuenta ni sus otros representados.
 */
@Entity
@Table(name = "representante_estudiante", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepresentanteEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_representante_estudiante")
    private Long idRepresentanteEstudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_representante", nullable = false)
    private Representante representante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    /** Texto libre: "Padre", "Madre", "Tutor legal", etc. Opcional. */
    @Column(name = "relacion", length = 50)
    private String relacion;

    /**
     * Cuando un estudiante tiene mas de un representante activo, distingue
     * a quien contactar primero (p.ej. para notificaciones de lesion).
     */
    @Column(name = "contacto_principal", nullable = false)
    @Builder.Default
    private Boolean contactoPrincipal = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
