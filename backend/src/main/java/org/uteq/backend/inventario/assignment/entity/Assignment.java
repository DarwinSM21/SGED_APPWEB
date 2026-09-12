package org.uteq.backend.inventario.assignment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.common.Zones;
import org.uteq.backend.inventario.item.entity.Item;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "asignaciones", schema = "inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {
    public enum RecipientType { ESTUDIANTE, ENTRENADOR }
    public enum AssignmentStatus { ASIGNADO, DEVUELTO, PERDIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_articulo", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destinatario", nullable = false, length = 15)
    private RecipientType recipientType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrenador")
    private Coach coach;

    @Column(name = "fecha_asignacion", nullable = false)
    @Builder.Default
    private LocalDate assignmentDate = LocalDate.now(Zones.ECUADOR);

    @Column(name = "fecha_devolucion_esperada")
    private LocalDate expectedReturnDate;

    @Column(name = "fecha_devolucion_real")
    private LocalDate actualReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASIGNADO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id_usuario", nullable = false)
    private UserAccount registeredBy;

    @Column(length = 255)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
