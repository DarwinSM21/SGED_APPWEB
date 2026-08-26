package org.uteq.backend.deportivo.asistencia.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long>, JpaSpecificationExecutor<Asistencia> {

    Optional<Asistencia> findBySesionIdSesionAndEstudianteIdEstudiante(Long idSesion, Long idEstudiante);

    List<Asistencia> findBySesionIdSesion(Long idSesion);

    /** Historial propio del estudiante, mas reciente primero. */
    Page<Asistencia> findByEstudiante_IdEstudianteOrderBySesion_FechaDesc(Long idEstudiante, Pageable pageable);

    /** Estudiantes habilitados para ser calificados en una sesion. */
    @Query("""
           SELECT a FROM Asistencia a
           WHERE a.sesion.idSesion = :idSesion
             AND a.estado IN ('PRESENTE', 'TARDE')
           """)
    List<Asistencia> listarHabilitadosParaEvaluar(@Param("idSesion") Long idSesion);

    @Query("""
           SELECT COUNT(a) FROM Asistencia a
           WHERE a.estudiante.idEstudiante = :idEstudiante
             AND a.estado IN ('PRESENTE', 'TARDE')
             AND a.sesion.fecha >= :desde
           """)
    long contarAsistenciasDesde(@Param("idEstudiante") Long idEstudiante, @Param("desde") LocalDate desde);

    /** Falso (o null) si el estudiante y la sesion no comparten categoria: no deja marcar. */
    @Procedure(procedureName = "deportivo.sp_validar_categoria_estudiante_sesion")
    Boolean validarCategoriaCoincide(@Param("p_estudiante") Long idEstudiante, @Param("p_sesion") Long idSesion);

    /** Null si no hubo sesiones programadas para su categoria en el rango. */
    @Procedure(procedureName = "deportivo.sp_reporte_asistencia_estudiante")
    BigDecimal calcularPorcentajeAsistencia(
            @Param("p_estudiante") Long idEstudiante,
            @Param("p_desde") LocalDate desde,
            @Param("p_hasta") LocalDate hasta);

    /**
     * Asistencia de TODOS los estudiantes activos en una sola consulta.
     *
     * Existe porque el panel de alertas llamaba a
     * sp_reporte_asistencia_estudiante una vez por estudiante: con 8 alumnos
     * eran 8 llamadas y se notaba poco, pero medido con 2.008 el panel pasaba
     * de 0,16 s a 2,9 s. El procedimiento sigue siendo la respuesta correcta
     * para consultar a UN estudiante -lo usan la ficha y el informe al
     * representante-; lo que no escala es invocarlo en bucle.
     *
     * Devuelve, por estudiante: programadas y presentes. El calculo del
     * porcentaje queda en el servicio para no repetir aqui la regla de que
     * cero programadas significa "sin dato" y no "cero por ciento".
     *
     * Es nativa y no JPQL porque necesita CTEs, que JPQL no expresa. La
     * primera version usaba dos subconsultas correlacionadas y seguia
     * tardando 1,4 s con 2.008 alumnos: el plan mostraba loops=2008, o sea
     * que Postgres las ejecutaba una vez por estudiante. Con CTEs cada
     * agregado se calcula UNA vez y despues se une.
     *
     * Van separadas porque cuentan cosas distintas: el denominador es por
     * CATEGORIA -las sesiones programadas del grupo- y el numerador por
     * ESTUDIANTE. Unirlos en una sola agrupacion multiplicaria filas.
     *
     * El JOIN con programadas es interior a proposito: si la categoria no
     * tuvo sesiones en la ventana, el estudiante no aparece y el servicio lo
     * lee como "sin dato" en vez de como cero por ciento.
     *
     * No es SQL dinamico: es una consulta fija con parametros nombrados, sin
     * concatenacion, que es lo que audita scripts/audit-sql-dynamic.sh.
     */
    @Query(value = """
           WITH programadas AS (
               SELECT id_categoria, COUNT(*) AS n
                 FROM deportivo.sesiones_entrenamiento
                WHERE fecha BETWEEN :desde AND :corte
                GROUP BY id_categoria
           ),
           presentes AS (
               SELECT a.id_estudiante, COUNT(*) AS n
                 FROM deportivo.asistencias a
                 JOIN deportivo.sesiones_entrenamiento se ON se.id_sesion = a.id_sesion
                WHERE a.estado IN ('PRESENTE', 'TARDE')
                  AND se.fecha BETWEEN :desde AND :corte
                GROUP BY a.id_estudiante
           )
           SELECT e.id_estudiante, pr.n, COALESCE(pe.n, 0)
             FROM academico.estudiantes e
             JOIN programadas pr ON pr.id_categoria = e.id_categoria
             LEFT JOIN presentes pe ON pe.id_estudiante = e.id_estudiante
            WHERE e.activo
           """, nativeQuery = true)
    List<Object[]> resumenAsistenciaDeActivos(
            @Param("desde") LocalDate desde, @Param("corte") LocalDate corte);

    /**
     * Presencias de varios estudiantes en una ventana, en UNA consulta.
     *
     * <p>Alimenta la convocatoria del partido junto con el promedio: sirve
     * para no titularizar a quien lleva semanas sin aparecer por mas alto que
     * tenga el promedio de cuando venia.
     */
    @Query("""
           SELECT a.estudiante.idEstudiante, COUNT(a)
           FROM Asistencia a
           WHERE a.estudiante.idEstudiante IN :ids
             AND a.estado IN ('PRESENTE', 'TARDE')
             AND a.sesion.fecha BETWEEN :desde AND :hasta
           GROUP BY a.estudiante.idEstudiante
           """)
    List<Object[]> presenciasEnVentana(@Param("ids") List<Long> ids,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    /** Asistencia de una sesion con el estudiante y su persona ya cargados. */
    @Query("""
           SELECT a FROM Asistencia a
           JOIN FETCH a.estudiante e
           JOIN FETCH e.persona
           LEFT JOIN FETCH e.posicion
           WHERE a.sesion.idSesion = :idSesion
           ORDER BY e.persona.apellido ASC
           """)
    List<Asistencia> historialDeSesion(@Param("idSesion") Long idSesion);
}
