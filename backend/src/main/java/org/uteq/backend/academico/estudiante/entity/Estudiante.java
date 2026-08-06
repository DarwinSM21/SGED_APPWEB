package org.uteq.backend.academico.estudiante.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.deportivo.categoria.entity.Categoria; // Revisa tu paquete de Categoria
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

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
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante")
    private Long idEstudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_general", nullable = false)
    private EstadoGeneral estadoGeneral;

    /**
     * Posicion habitual del jugador. La columna existe desde la migracion V3
     * (ALTER TABLE academico.estudiantes ADD COLUMN id_posicion) pero nunca
     * se habia mapeado en la entidad. Es la posicion nominal, no
     * necesariamente la que jugo un dia concreto — para eso esta
     * EvaluacionEstudiante.posicionJugada, que puede diferir.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_posicion")
    private Posicion posicion;

    /**
     * Cuenta de acceso propia, opcional: la inmensa mayoria de estudiantes no
     * la tiene. Un administrador la habilita explicitamente (POST
     * /api/estudiantes/{id}/acceso) sobre la Persona que este estudiante YA
     * tiene, sin crear una Persona duplicada. Es lo que le permite marcar su
     * propia asistencia escaneando el QR de recepcion.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @Column(name = "codigo_estudiante", nullable = false, unique = true, length = 30)
    private String codigoEstudiante;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "peso", precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(name = "altura", precision = 5, scale = 2)
    private BigDecimal altura;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
} 