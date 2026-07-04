package org.uteq.backend.deportivo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.deportivo.entity.EvaluacionDiaria;
import org.uteq.backend.deportivo.entity.EvaluacionDiaria.EstadoEvaluacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionDiariaRepository extends JpaRepository<EvaluacionDiaria, Long> {

    Optional<EvaluacionDiaria> findBySesionIdSesion(Long idSesion);

    Page<EvaluacionDiaria> findByFecha(LocalDate fecha, Pageable pageable);

    Page<EvaluacionDiaria> findByEntrenadorIdEntrenador(Long idEntrenador, Pageable pageable);

    @Query("SELECT e FROM EvaluacionDiaria e WHERE e.entrenador.idEntrenador = :idEntrenador " +
           "AND e.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY e.fecha DESC")
    List<EvaluacionDiaria> findByEntrenadorAndFechaRange(
            @Param("idEntrenador") Long idEntrenador,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    Page<EvaluacionDiaria> findByEstado(EstadoEvaluacion estado, Pageable pageable);

    long countByEstado(EstadoEvaluacion estado);
}
