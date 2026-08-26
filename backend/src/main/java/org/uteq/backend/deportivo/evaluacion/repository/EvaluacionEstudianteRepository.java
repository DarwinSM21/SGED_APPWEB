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

    /**
     * Promedio de cada estudiante en una ventana de fechas, mirando la fecha
     * de la SESION evaluada y no la de creacion de la fila: una evaluacion
     * cargada tarde sigue perteneciendo al dia que se entreno.
     *
     * <p>Es lo que alimenta la convocatoria de un partido. El promedio
     * historico completo -promedioGeneralPorEstudiante- no sirve ahi: premia
     * al que jugo bien hace un anio por encima del que viene mejor ahora, que
     * es justamente lo contrario de lo que el entrenador necesita para decidir
     * con quien sale el sabado.
     */
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
