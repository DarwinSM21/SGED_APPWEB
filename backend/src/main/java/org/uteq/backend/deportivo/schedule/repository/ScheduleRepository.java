package org.uteq.backend.deportivo.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.schedule.entity.Schedule;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT h FROM Schedule h WHERE h.entrenador.idEntrenador = :idEntrenador AND h.activo = true ORDER BY h.diaSemana ASC, h.horaInicio ASC")
    List<Schedule> findActiveByCoachOrderByDayAndStartTime(@Param("idEntrenador") Long idEntrenador);

    @Query("SELECT h FROM Schedule h WHERE h.activo = true AND h.diaSemana = :diaSemana")
    List<Schedule> findActiveByDayOfWeek(@Param("diaSemana") Short diaSemana);

    @Query("SELECT h FROM Schedule h WHERE h.idHorario = :idHorario AND h.entrenador.idEntrenador = :idEntrenador")
    Optional<Schedule> findByIdAndCoachId(@Param("idHorario") Long idHorario, @Param("idEntrenador") Long idEntrenador);

    @Query("""
           SELECT h FROM Schedule h
            JOIN FETCH h.categoria
            WHERE h.entrenador.idEntrenador = :idEntrenador
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
