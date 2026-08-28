package org.uteq.backend.deportivo.horario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.horario.entity.Horario;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HorarioRepository extends JpaRepository<Horario, Long> {

    List<Horario> findByEntrenador_IdEntrenadorAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(Long idEntrenador);

    /** Alimenta la generacion automatica de sesiones del dia: todo horario activo que cae hoy. */
    List<Horario> findByActivoTrueAndDiaSemana(Short diaSemana);

    Optional<Horario> findByIdHorarioAndEntrenador_IdEntrenador(Long idHorario, Long idEntrenador);

    /**
     * El otro horario ACTIVO del mismo entrenador que se cruza con el rango,
     * si lo hay.
     *
     * <p>Devuelve el horario y no un booleano porque la escuela tiene POCOS
     * entrenadores: uno solo cubre varias categorias, asi que al reorganizar
     * la semana se va a chocar seguido. Un "no se puede" a secas obliga a ir
     * a buscar cual era; devolviendo el culpable, el mensaje puede decir con
     * que choca y el arreglo es inmediato.
     *
     * <p>Se valida el entrenador y no la cancha a proposito: una cancha se
     * puede compartir entre dos grupos, una persona no.
     *
     * <p>Dos rangos se cruzan si cada uno empieza antes de que termine el
     * otro. Tocarse por el extremo -uno termina 18:00 y el otro empieza a las
     * 18:00- no es cruzarse, y es justamente como un entrenador encadena dos
     * categorias seguidas.
     *
     * <p>idExcluir deja fuera el propio horario al editarlo; para un alta se
     * pasa un id imposible en vez de null, porque "<> null" en SQL no compara
     * como uno espera y descartaria todas las filas.
     */
    @Query("""
           SELECT h FROM Horario h
            JOIN FETCH h.categoria
            WHERE h.entrenador.idEntrenador = :idEntrenador
              AND h.diaSemana = :diaSemana
              AND h.activo = true
              AND h.idHorario <> :idExcluir
              AND h.horaInicio < :horaFin
              AND h.horaFin > :horaInicio
            ORDER BY h.horaInicio
           """)
    List<Horario> cruzadosCon(@Param("idEntrenador") Long idEntrenador,
                              @Param("diaSemana") Short diaSemana,
                              @Param("horaInicio") LocalTime horaInicio,
                              @Param("horaFin") LocalTime horaFin,
                              @Param("idExcluir") Long idExcluir);
}
