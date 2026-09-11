package org.uteq.backend.deportivo.evaluacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluacion.entity.EvaluacionEstudiante;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EvaluacionEstudianteRepository extends JpaRepository<EvaluacionEstudiante, Long>, JpaSpecificationExecutor<EvaluacionEstudiante> {
    @Query("SELECT ee FROM EvaluacionEstudiante ee WHERE ee.evaluacion.idEvaluacion = :idEvaluacion AND ee.estudiante.id = :idEstudiante")
    Optional<EvaluacionEstudiante> findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(
            @Param("idEvaluacion") Long idEvaluacion, @Param("idEstudiante") Long idEstudiante);

    @Query("""
           SELECT c.nombre, AVG(d.puntaje)
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           JOIN d.criterio c
           WHERE ee.estudiante.id = :idEstudiante
           GROUP BY c.nombre
           """)
    List<Object[]> promedioHistoricoPorCriterio(@Param("idEstudiante") Long idEstudiante);

    @Query("""
           SELECT c.nombre, d.puntaje
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           JOIN d.criterio c
           WHERE ee.estudiante.id = :idEstudiante
             AND ee.evaluacion.idEvaluacion = :idEvaluacionPrevia
           """)
    List<Object[]> puntajesDeEvaluacion(@Param("idEstudiante") Long idEstudiante,
                                        @Param("idEvaluacionPrevia") Long idEvaluacionPrevia);

    @Query("""
           SELECT ee.estudiante.id, AVG(d.puntaje)
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           WHERE ee.estudiante.id IN :ids
           GROUP BY ee.estudiante.id
           """)
    List<Object[]> promedioGeneralPorEstudiante(@Param("ids") List<Long> ids);

    @Query("""
           SELECT ee.estudiante.id, AVG(d.puntaje)
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           WHERE ee.estudiante.id IN :ids
             AND ee.evaluacion.sesion.fecha BETWEEN :desde AND :hasta
           GROUP BY ee.estudiante.id
           """)
    List<Object[]> promedioEnVentana(@Param("ids") List<Long> ids,
                                     @Param("desde") LocalDate desde,
                                     @Param("hasta") LocalDate hasta);
}
