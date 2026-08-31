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

    List<Horario> findByActivoTrueAndDiaSemana(Short diaSemana);

    Optional<Horario> findByIdHorarioAndEntrenador_IdEntrenador(Long idHorario, Long idEntrenador);

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
