package org.uteq.backend.deportivo.evaluacion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.posicion.entity.Posicion;

import java.time.Instant;

@Entity
@Table(name = "alineacion_jugador", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlineacionJugador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alineacion_jugador")
    private Long idAlineacionJugador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alineacion", nullable = false)
    private Alineacion alineacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_posicion")
    private Posicion posicion;

    @Column(name = "titular", nullable = false)
    @Builder.Default
    private Boolean titular = true;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = Instant.now();
    }
}
