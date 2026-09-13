package org.uteq.backend.deportivo.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluation.entity.StudentEvaluation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a los puntajes de un jugador dentro de una evaluación diaria, y a
 * los promedios agregados (histórico por criterio, comparación entre
 * evaluaciones, resumen por rango de fechas) usados en fichas y reportes.
 */
public interface StudentEvaluationRepository extends JpaRepository<StudentEvaluation, Long>, JpaSpecificationExecutor<StudentEvaluation> {
    /**
     * @param idEvaluacion identificador de la evaluación diaria
     * @param idEstudiante identificador del estudiante
     * @return los puntajes de ese estudiante en esa evaluación, si existen
     */
    @Query("SELECT ee FROM StudentEvaluation ee WHERE ee.evaluacion.idEvaluacion = :idEvaluacion AND ee.estudiante.id = :idEstudiante")
    Optional<StudentEvaluation> findByEvaluation_IdAndStudent_Id(
            @Param("idEvaluacion") Long idEvaluacion, @Param("idEstudiante") Long idEstudiante);

    /**
     * @param idEstudiante identificador del estudiante
     * @return filas {@code [nombre del criterio, promedio histórico]} de todas las evaluaciones de ese estudiante
     */
    @Query("""
           SELECT c.nombre, AVG(d.puntaje)
           FROM EvaluationDetail d
           JOIN d.evaluacionEstudiante ee
           JOIN d.criterio c
           WHERE ee.estudiante.id = :idEstudiante
           GROUP BY c.nombre
           """)
    List<Object[]> historicalAverageByCriterion(@Param("idEstudiante") Long idEstudiante);

    /**
     * @param idEstudiante identificador del estudiante
     * @param idEvaluacionPrevia identificador de la evaluación diaria cuyos puntajes se consultan
     * @return filas {@code [nombre del criterio, puntaje]} de ese estudiante en esa evaluación
     */
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

    /**
     * @param ids identificadores de los estudiantes a considerar
     * @return filas {@code [idEstudiante, promedio general de puntaje]} de esos estudiantes
     */
    @Query("""
           SELECT ee.estudiante.id, AVG(d.puntaje)
           FROM EvaluationDetail d
           JOIN d.evaluacionEstudiante ee
           WHERE ee.estudiante.id IN :ids
           GROUP BY ee.estudiante.id
           """)
    List<Object[]> overallAverageByStudent(@Param("ids") List<Long> ids);

    /**
     * @param ids identificadores de los estudiantes a considerar
     * @param desde fecha inicial del rango, inclusive
     * @param hasta fecha final del rango, inclusive
     * @return filas {@code [idEstudiante, promedio de puntaje]} de esos estudiantes en ese rango de fechas
     */
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
