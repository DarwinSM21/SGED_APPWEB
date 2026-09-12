package org.uteq.backend.deportivo.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluation.entity.StudentEvaluation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentEvaluationRepository extends JpaRepository<StudentEvaluation, Long>, JpaSpecificationExecutor<StudentEvaluation> {
    @Query("SELECT ee FROM StudentEvaluation ee WHERE ee.evaluacion.idEvaluacion = :idEvaluacion AND ee.estudiante.id = :idEstudiante")
    Optional<StudentEvaluation> findByEvaluation_IdAndStudent_Id(
            @Param("idEvaluacion") Long idEvaluacion, @Param("idEstudiante") Long idEstudiante);

    @Query("""
           SELECT c.nombre, AVG(d.puntaje)
           FROM EvaluationDetail d
           JOIN d.evaluacionEstudiante ee
           JOIN d.criterio c
           WHERE ee.estudiante.id = :idEstudiante
           GROUP BY c.nombre
           """)
    List<Object[]> historicalAverageByCriterion(@Param("idEstudiante") Long idEstudiante);

    @Query("""
           SELECT c.nombre, d.puntaje
           FROM EvaluationDetail d
           JOIN d.evaluacionEstudiante ee
           JOIN d.criterio c
           WHERE ee.estudiante.id = :idEstudiante
             AND ee.evaluacion.idEvaluacion = :idEvaluacionPrevia
           """)
    List<Object[]> scoresForEvaluation(@Param("idEstudiante") Long idEstudiante,
                                        @Param("idEvaluacionPrevia") Long idEvaluacionPrevia);

    @Query("""
           SELECT ee.estudiante.id, AVG(d.puntaje)
           FROM EvaluationDetail d
           JOIN d.evaluacionEstudiante ee
           WHERE ee.estudiante.id IN :ids
           GROUP BY ee.estudiante.id
           """)
    List<Object[]> overallAverageByStudent(@Param("ids") List<Long> ids);

    @Query("""
           SELECT ee.estudiante.id, AVG(d.puntaje)
           FROM EvaluationDetail d
           JOIN d.evaluacionEstudiante ee
           WHERE ee.estudiante.id IN :ids
             AND ee.evaluacion.sesion.fecha BETWEEN :desde AND :hasta
           GROUP BY ee.estudiante.id
           """)
    List<Object[]> averageInWindow(@Param("ids") List<Long> ids,
                                     @Param("desde") LocalDate desde,
                                     @Param("hasta") LocalDate hasta);
}
