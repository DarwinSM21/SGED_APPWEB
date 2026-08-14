package org.uteq.backend.deportivo.horario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.horario.entity.Horario;

import java.util.List;
import java.util.Optional;

public interface HorarioRepository extends JpaRepository<Horario, Long> {

    List<Horario> findByEntrenador_IdEntrenadorAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(Long idEntrenador);

    /** Alimenta la generacion automatica de sesiones del dia: todo horario activo que cae hoy. */
    List<Horario> findByActivoTrueAndDiaSemana(Short diaSemana);

    Optional<Horario> findByIdHorarioAndEntrenador_IdEntrenador(Long idHorario, Long idEntrenador);
}
