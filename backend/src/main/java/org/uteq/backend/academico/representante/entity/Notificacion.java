package org.uteq.backend.academico.representante.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.uteq.backend.academico.estudiante.entity.Estudiante;

import java.time.Instant;

/**
 * Notificacion en-app al representante (RF-22): se crea una fila por cada
 * representante con vinculo activo al estudiante cuando este marca
 * asistencia o se le registra una lesion. No hay envio de correo/SMS: no
 * existe infraestructura externa para eso en este proyecto.
 */
@Entity
@Table(name = "notificaciones", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    public enum Tipo { ASISTENCIA, LESION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_representante", nullable = false)
    private Representante representante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
