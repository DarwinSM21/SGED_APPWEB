package org.uteq.backend.deportivo.evaluacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluacion.entity.EvaluacionEstudiante;

import java.util.List;
import java.util.Optional;

public interface EvaluacionEstudianteRepository extends JpaRepository<EvaluacionEstudiante, Long> {

    Optional<EvaluacionEstudiante> findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(
            Long idEvaluacion, Long idEstudiante);

    /**
     * Promedio historico por criterio de un estudiante. Alimenta dos cosas: la
     * precarga del dia siguiente y el contexto que se manda al modelo de
     * lenguaje para que pueda hablar de evolucion y no solo del dia suelto.
     */
    @Query("""
           SELECT c.nombre, AVG(d.puntaje)
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           JOIN d.criterio c
           WHERE ee.estudiante.idEstudiante = :idEstudiante
           GROUP BY c.nombre
           """)
    List<Object[]> promedioHistoricoPorCriterio(@Param("idEstudiante") Long idEstudiante);

    /**
     * Puntajes del estudiante en la evaluacion inmediatamente anterior.
     * El documento del modulo lo pide explicitamente: "cada dia arranca con
     * los valores del dia anterior ya cargados".
     */
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

    /** Promedio general acumulado, base del ranking deterministico de la plantilla. */
    @Query("""
           SELECT ee.estudiante.idEstudiante, AVG(d.puntaje)
           FROM DetalleEvaluacion d
           JOIN d.evaluacionEstudiante ee
           WHERE ee.estudiante.idEstudiante IN :ids
           GROUP BY ee.estudiante.idEstudiante
           """)
    List<Object[]> promedioGeneralPorEstudiante(@Param("ids") List<Long> ids);
}
