package org.uteq.backend.deportivo.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.schedule.entity.Schedule;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a los horarios fijos de entrenamiento (día de la semana, hora,
 * entrenador, categoría) a partir de los cuales se generan las sesiones.
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    /**
     * @param idEntrenador identificador del entrenador
     * @return los horarios activos de ese entrenador, ordenados por día y hora de inicio
     */
    @Query("SELECT h FROM Schedule h WHERE h.entrenador.coachId = :idEntrenador AND h.activo = true ORDER BY h.diaSemana ASC, h.horaInicio ASC")
    List<Schedule> findActiveByCoachOrderByDayAndStartTime(@Param("idEntrenador") Long idEntrenador);

    /**
     * @param diaSemana día de la semana a filtrar (1 = lunes ... 7 = domingo)
     * @return los horarios activos de ese día
     */
    @Query("SELECT h FROM Schedule h WHERE h.activo = true AND h.diaSemana = :diaSemana")
    List<Schedule> findActiveByDayOfWeek(@Param("diaSemana") Short diaSemana);

    /**
     * Busca un horario puntual y comprueba a la vez que pertenece al
     * entrenador, para no permitir editar o quitar un horario ajeno.
     *
     * @param idHorario identificador del horario
     * @param idEntrenador identificador del entrenador que lo solicita
     * @return el horario, si existe y pertenece a ese entrenador
     */
    @Query("SELECT h FROM Schedule h WHERE h.idHorario = :idHorario AND h.entrenador.coachId = :idEntrenador")
    Optional<Schedule> findByIdAndCoachId(@Param("idHorario") Long idHorario, @Param("idEntrenador") Long idEntrenador);

    /**
     * Horarios activos del mismo entrenador y día que se cruzan con un rango
     * horario dado, para rechazar la creación/edición de un horario que
     * chocaría con otro (un entrenador no puede estar en dos canchas a la vez).
     *
     * @param idEntrenador identificador del entrenador
     * @param diaSemana día de la semana a comprobar
     * @param horaInicio hora de inicio del rango propuesto
     * @param horaFin hora de fin del rango propuesto
     * @param idExcluir identificador del horario que se excluye de la comprobación (el que se está editando, o un valor inexistente al crear)
     * @return los horarios existentes que se cruzan con el rango propuesto
     */
    @Query("""
           SELECT h FROM Schedule h
            JOIN FETCH h.categoria
            WHERE h.entrenador.coachId = :idEntrenador
              AND h.diaSemana = :diaSemana
              AND h.activo = true
              AND h.idHorario <> :idExcluir
              AND h.horaInicio < :horaFin
              AND h.horaFin > :horaInicio
            ORDER BY h.horaInicio
           """)
    List<Schedule> overlapsWith(@Param("idEntrenador") Long idEntrenador,
                              @Param("diaSemana") Short diaSemana,
                              @Param("horaInicio") LocalTime horaInicio,
                              @Param("horaFin") LocalTime horaFin,
                              @Param("idExcluir") Long idExcluir);
}
