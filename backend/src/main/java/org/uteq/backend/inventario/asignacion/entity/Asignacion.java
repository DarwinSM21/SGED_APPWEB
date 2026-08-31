package org.uteq.backend.inventario.asignacion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "asignaciones", schema = "inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asignacion {
    public enum TipoDestinatario { ESTUDIANTE, ENTRENADOR }
    public enum EstadoAsignacion { ASIGNADO, DEVUELTO, PERDIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Long idAsignacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_articulo", nullable = false)
    private Articulo articulo;

    @Column(nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destinatario", nullable = false, length = 15)
    private TipoDestinatario tipoDestinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrenador")
    private Entrenador entrenador;

    @Column(name = "fecha_asignacion", nullable = false)
    @Builder.Default
    private LocalDate fechaAsignacion = LocalDate.now(Zonas.ECUADOR);

    @Column(name = "fecha_devolucion_esperada")
    private LocalDate fechaDevolucionEsperada;

    @Column(name = "fecha_devolucion_real")
    private LocalDate fechaDevolucionReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoAsignacion estado = EstadoAsignacion.ASIGNADO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id_usuario", nullable = false)
    private Usuario registradoPor;

    @Column(length = 255)
    private String observaciones;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
