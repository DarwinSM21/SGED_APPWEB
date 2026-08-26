package org.uteq.backend.deportivo.partido.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uteq.backend.deportivo.categoria.entity.Categoria;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un partido de la academia.
 *
 * <p>Es la contraparte de {@link org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento}:
 * la sesion es donde se mide -asistencia y evaluacion- y el partido es donde
 * se usa lo medido. La alineacion cuelga de aqui y no de la sesion porque
 * decidir con quien se sale a jugar no es un hecho del entrenamiento; atarla
 * a la sesion obligaba ademas a que solo pudieran alinearse los que fueron a
 * ESE entrenamiento, cuando lo que corresponde mirar para un partido es el
 * rendimiento de las semanas anteriores.
 *
 * <p>Solo se guarda el marcador propio. El sistema es de UNA academia:
 * "local" y "visitante" pediria un catalogo de rivales que nadie va a
 * mantener, y lo que hace falta saber es si se gano.
 */
@Entity
@Table(name = "partidos", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partido")
    private Long idPartido;

    /** La SUB-14 no juega el partido de la SUB-17: de aqui salen los convocables. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column
    private LocalTime hora;

    /**
     * Nulos mientras no se juegue. Null es "sin resultado" y 0 es "no metio
     * ninguno": son cosas distintas y por eso no hay valor por defecto.
     */
    @Column(name = "goles_favor")
    private Short golesFavor;

    @Column(name = "goles_contra")
    private Short golesContra;

    @Column(length = 500)
    private String observacion;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    /** true cuando ya se cargo el marcador. */
    @Transient
    public boolean tieneResultado() {
        return golesFavor != null && golesContra != null;
    }
}
