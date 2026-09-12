package org.uteq.backend.deportivo.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.deportivo.position.entity.Position;

import java.time.Instant;

@Entity
@Table(name = "alineacion_jugador", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LineupPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alineacion_jugador")
    private Long idAlineacionJugador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alineacion", nullable = false)
    private Lineup alineacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Student estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_posicion")
    private Position posicion;

    @Column(name = "titular", nullable = false)
    @Builder.Default
    private Boolean titular = true;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @PrePersist
    void onCreate() {
        creadoEn = Instant.now();
    }
}
