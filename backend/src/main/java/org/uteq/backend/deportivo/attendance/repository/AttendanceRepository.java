package org.uteq.backend.deportivo.attendance.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.attendance.entity.Attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a las marcas de asistencia por sesión de entrenamiento, incluidos
 * los cálculos agregados (porcentaje individual, resumen por categoría) que
 * alimentan el panel de alertas y los reportes.
 */
public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {
    /**
     * @param idSesion identificador de la sesión de entrenamiento
     * @param idEstudiante identificador del estudiante
     * @return la marca de asistencia de ese estudiante en esa sesión, si existe
     */
    @Query("SELECT a FROM Attendance a WHERE a.sesion.idSesion = :idSesion AND a.estudiante.id = :idEstudiante")
    Optional<Attendance> findBySession_IdAndStudent_Id(@Param("idSesion") Long idSesion, @Param("idEstudiante") Long idEstudiante);

    /**
     * @param idSesion identificador de la sesión de entrenamiento
     * @return las marcas de asistencia registradas en esa sesión
     */
    @Query("SELECT a FROM Attendance a WHERE a.sesion.idSesion = :idSesion")
    List<Attendance> findBySession_Id(@Param("idSesion") Long idSesion);

    /**
     * @param idEstudiante identificador del estudiante
     * @param pageable página y tamaño solicitados
     * @return el historial de asistencia de ese estudiante, de la sesión más reciente a la más antigua
     */
    @Query("SELECT a FROM Attendance a WHERE a.estudiante.id = :idEstudiante ORDER BY a.sesion.fecha DESC")
    Page<Attendance> findByStudent_IdOrderBySession_DateDesc(@Param("idEstudiante") Long idEstudiante, Pageable pageable);

    /**
     * @param idSesion identificador de la sesión de entrenamiento
     * @return los estudiantes marcados presentes o tarde en esa sesión, evaluables en la sesión diaria
     */
    @Query("""
           SELECT a FROM Attendance a
           WHERE a.sesion.idSesion = :idSesion
             AND a.estado IN ('PRESENT', 'LATE')
           """)
    List<Attendance> findEligibleForEvaluation(@Param("idSesion") Long idSesion);

    /**
     * @param idEstudiante identificador del estudiante
     * @param desde fecha desde la que se cuenta, inclusive
     * @return la cantidad de sesiones en que el estudiante estuvo presente o llegó tarde desde esa fecha
     */
    @Query("""
           SELECT COUNT(a) FROM Attendance a
           WHERE a.estudiante.id = :idEstudiante
             AND a.estado IN ('PRESENT', 'LATE')
             AND a.sesion.fecha >= :desde
           """)
    long countSince(@Param("idEstudiante") Long idEstudiante, @Param("desde") LocalDate desde);

    /**
     * Invoca el procedimiento almacenado que valida que un estudiante
     * pertenezca a la categoría de una sesión, usado al marcar asistencia
     * por QR para rechazar a estudiantes de otra categoría.
     *
     * @param idEstudiante identificador del estudiante
     * @param idSesion identificador de la sesión de entrenamiento
     * @return {@code true} si el estudiante pertenece a la categoría de la sesión
     */
    @Procedure(procedureName = "deportivo.sp_validar_categoria_estudiante_sesion")
    Boolean matchesCategory(@Param("p_estudiante") Long idEstudiante, @Param("p_sesion") Long idSesion);

    /**
     * Invoca el procedimiento almacenado que calcula el porcentaje de
     * asistencia (presente o tarde sobre sesiones programadas) de un
     * estudiante en un rango de fechas.
     *
     * @param idEstudiante identificador del estudiante
     * @param desde fecha inicial del rango, inclusive
     * @param hasta fecha final del rango, inclusive
     * @return el porcentaje de asistencia calculado por el procedimiento
     */
    @Procedure(procedureName = "deportivo.sp_reporte_asistencia_estudiante")
    BigDecimal calculateAttendancePercentage(
            @Param("p_estudiante") Long idEstudiante,
            @Param("p_desde") LocalDate desde,
            @Param("p_hasta") LocalDate hasta);

    /**
     * Resumen agregado, por estudiante activo, de sesiones programadas y
     * presencias registradas en un rango de fechas (usado por el panel de
     * alertas para detectar estudiantes en riesgo de abandono).
     *
     * @param desde fecha inicial del rango, inclusive
     * @param corte fecha final del rango, inclusive
     * @return filas {@code [idEstudiante, sesiones programadas, sesiones con presencia]} por estudiante activo
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
                WHERE a.estado IN ('PRESENT', 'LATE')
                  AND se.fecha BETWEEN :desde AND :corte
                GROUP BY a.id_estudiante
           )
           SELECT e.id_estudiante, pr.n, COALESCE(pe.n, 0)
             FROM academico.estudiantes e
             JOIN programadas pr ON pr.id_categoria = e.id_categoria
             LEFT JOIN presentes pe ON pe.id_estudiante = e.id_estudiante
            WHERE e.activo
           """, nativeQuery = true)
    List<Object[]> activeSummary(
            @Param("desde") LocalDate desde, @Param("corte") LocalDate corte);

    /**
     * @param ids identificadores de los estudiantes a considerar
     * @param desde fecha inicial del rango, inclusive
     * @param hasta fecha final del rango, inclusive
     * @return filas {@code [idEstudiante, cantidad de presencias]} de esos estudiantes en el rango
     */
    @Query("""
           SELECT a.estudiante.id, COUNT(a)
           FROM Attendance a
           WHERE a.estudiante.id IN :ids
             AND a.estado IN ('PRESENT', 'LATE')
             AND a.sesion.fecha BETWEEN :desde AND :hasta
           GROUP BY a.estudiante.id
           """)
    List<Object[]> presencesInWindow(@Param("ids") List<Long> ids,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    /**
     * @param idSesion identificador de la sesión de entrenamiento
     * @return la nómina de esa sesión con persona y posición precargadas, ordenada por apellido
     */
    @Query("""
           SELECT a FROM Attendance a
           JOIN FETCH a.estudiante e
           JOIN FETCH e.person
           LEFT JOIN FETCH e.position
           WHERE a.sesion.idSesion = :idSesion
           ORDER BY e.person.lastName ASC
           """)
    List<Attendance> sessionHistory(@Param("idSesion") Long idSesion);
}
