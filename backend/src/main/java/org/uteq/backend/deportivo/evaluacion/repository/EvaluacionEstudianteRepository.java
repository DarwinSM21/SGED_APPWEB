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
    Optional<EvaluacionEstudiante> findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(
            Long idEvaluacion, Long idEstudiante);

    @Query("""
           SELECT c.nombre, AVG(d.puntaje)
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           JOIN d.criterio c
           WHERE ee.estudiante.idEstudiante = :idEstudiante
           GROUP BY c.nombre
           """)
    List<Object[]> promedioHistoricoPorCriterio(@Param("idEstudiante") Long idEstudiante);

    @Query("""
           SELECT c.nombre, d.puntaje
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           JOIN d.criterio c
           WHERE ee.estudiante.idEstudiante = :idEstudiante
             AND ee.evaluacion.idEvaluacion = :idEvaluacionPrevia
           """)
    List<Object[]> puntajesDeEvaluacion(@Param("idEstudiante") Long idEstudiante,
                                        @Param("idEvaluacionPrevia") Long idEvaluacionPrevia);

    @Query("""
           SELECT ee.estudiante.idEstudiante, AVG(d.puntaje)
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           WHERE ee.estudiante.idEstudiante IN :ids
           GROUP BY ee.estudiante.idEstudiante
           """)
    List<Object[]> promedioGeneralPorEstudiante(@Param("ids") List<Long> ids);

    @Query("""
           SELECT ee.estudiante.idEstudiante, AVG(d.puntaje)
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           WHERE ee.estudiante.idEstudiante IN :ids
             AND ee.evaluacion.sesion.fecha BETWEEN :desde AND :hasta
           GROUP BY ee.estudiante.idEstudiante
           """)
    List<Object[]> promedioEnVentana(@Param("ids") List<Long> ids,
                                     @Param("desde") LocalDate desde,
                                     @Param("hasta") LocalDate hasta);
}
