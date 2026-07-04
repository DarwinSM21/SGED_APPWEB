package org.uteq.backend.deportivo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.deportivo.entity.DetalleEvaluacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DetalleEvaluacionRepository extends JpaRepository<DetalleEvaluacion, Long> {

    List<DetalleEvaluacion> findByEvaluacionIdEvaluacion(Long idEvaluacion);

    List<DetalleEvaluacion> findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(Long idEvaluacion, Long idEstudiante);

    Optional<DetalleEvaluacion> findByEvaluacionIdEvaluacionAndEstudianteIdEstudianteAndCriterioIdCriterio(
            Long idEvaluacion, Long idEstudiante, Long idCriterio);

    @Query("SELECT AVG(d.puntaje) FROM DetalleEvaluacion d WHERE d.evaluacion.idEvaluacion = :idEvaluacion " +
           "AND d.estudiante.idEstudiante = :idEstudiante")
    Optional<BigDecimal> findPromedioByEvaluacionAndEstudiante(
            @Param("idEvaluacion") Long idEvaluacion,
            @Param("idEstudiante") Long idEstudiante);

    @Query("SELECT d FROM DetalleEvaluacion d WHERE d.estudiante.idEstudiante = :idEstudiante " +
           "ORDER BY d.evaluacion.fecha DESC")
    List<DetalleEvaluacion> findHistoricoEstudiante(@Param("idEstudiante") Long idEstudiante);
}
