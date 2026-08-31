package org.uteq.backend.deportivo.evaluacion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.deportivo.partido.entity.Partido;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alineaciones", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alineacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alineacion")
    private Long idAlineacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_partido", nullable = false, unique = true)
    private Partido partido;

    @Column(name = "valoracion")
    private Short valoracion;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @OneToMany(mappedBy = "alineacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AlineacionJugador> jugadores = new ArrayList<>();

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = Instant.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }
}
