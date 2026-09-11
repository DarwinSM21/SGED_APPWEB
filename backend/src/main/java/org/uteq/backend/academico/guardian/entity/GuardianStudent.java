package org.uteq.backend.academico.guardian.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.student.entity.Student;

import java.time.Instant;

@Entity
@Table(name = "representante_estudiante", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianStudent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_representante_estudiante")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_representante", nullable = false)
    private Guardian guardian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Student student;

    @Column(name = "activo")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "relacion", length = 50)
    private String relationship;

    @Column(name = "contacto_principal", nullable = false)
    @Builder.Default
    private Boolean primaryContact = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
