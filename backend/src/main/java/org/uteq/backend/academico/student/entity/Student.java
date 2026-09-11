package org.uteq.backend.academico.student.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "estudiantes", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_general", nullable = false)
    private GeneralStatus generalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_posicion")
    private Posicion position;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private UserAccount userAccount;

    @Column(name = "codigo_estudiante", nullable = false, unique = true, length = 30)
    private String studentCode;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate enrollmentDate;

    @Column(name = "peso", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "altura", precision = 5, scale = 2)
    private BigDecimal height;

    @Column(name = "activo")
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
