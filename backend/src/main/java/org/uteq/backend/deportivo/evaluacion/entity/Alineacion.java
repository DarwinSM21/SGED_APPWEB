package org.uteq.backend.deportivo.evaluacion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * La alineacion que el entrenador puso realmente en la cancha.
 *
 * <p>No sustituye a la sugerencia: la sugerencia se sigue calculando en
 * PlantillaService a partir de asistencia, lesiones y promedio, y es lo que se
 * ofrece mientras nadie haya guardado nada aqui. Esta tabla registra la
 * decision del entrenador cuando existe, que es otra cosa: el entrenador mira
 * la sugerencia, mete al suplente que viene entrenando mejor, y juega con esa.
 *
 * <p>Una sesion tiene una sola alineacion. Guardar de nuevo la reemplaza en
 * vez de acumular versiones: lo que importa historicamente es con que once se
 * jugo, no cuantas veces se retoco antes de empezar.
 */
@Entity
@Table(name = "alineaciones", schema = "deportivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alineacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alineacion")
    private Long idAlineacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sesion", nullable = false, unique = true)
    private SesionEntrenamiento sesion;

    /**
     * Como le fue al equipo con este once, de 1 a 5. Nullable a proposito: la
     * alineacion se guarda antes de jugar y se califica despues, si es que se
     * califica. Obligarla al guardar forzaria al entrenador a puntuar un
     * partido que todavia no ocurrio.
     */
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
