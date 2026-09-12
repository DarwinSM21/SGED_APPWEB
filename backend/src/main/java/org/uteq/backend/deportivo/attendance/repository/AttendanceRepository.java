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

public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {
    @Query("SELECT a FROM Attendance a WHERE a.sesion.idSesion = :idSesion AND a.estudiante.id = :idEstudiante")
    Optional<Attendance> findBySession_IdAndStudent_Id(@Param("idSesion") Long idSesion, @Param("idEstudiante") Long idEstudiante);

    @Query("SELECT a FROM Attendance a WHERE a.sesion.idSesion = :idSesion")
    List<Attendance> findBySession_Id(@Param("idSesion") Long idSesion);

    @Query("SELECT a FROM Attendance a WHERE a.estudiante.id = :idEstudiante ORDER BY a.sesion.fecha DESC")
    Page<Attendance> findByStudent_IdOrderBySession_DateDesc(@Param("idEstudiante") Long idEstudiante, Pageable pageable);

    @Query("""
           SELECT a FROM Attendance a
           WHERE a.sesion.idSesion = :idSesion
             AND a.estado IN ('PRESENTE', 'TARDE')
           """)
    List<Attendance> findEligibleForEvaluation(@Param("idSesion") Long idSesion);

    @Query("""
           SELECT COUNT(a) FROM Attendance a
           WHERE a.estudiante.id = :idEstudiante
             AND a.estado IN ('PRESENTE', 'TARDE')
             AND a.sesion.fecha >= :desde
           """)
    long countSince(@Param("idEstudiante") Long idEstudiante, @Param("desde") LocalDate desde);

    @Procedure(procedureName = "deportivo.sp_validar_categoria_estudiante_sesion")
    Boolean matchesCategory(@Param("p_estudiante") Long idEstudiante, @Param("p_sesion") Long idSesion);

    @Procedure(procedureName = "deportivo.sp_reporte_asistencia_estudiante")
    BigDecimal calculateAttendancePercentage(
            @Param("p_estudiante") Long idEstudiante,
            @Param("p_desde") LocalDate desde,
            @Param("p_hasta") LocalDate hasta);

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
    List<Object[]> activeSummary(
            @Param("desde") LocalDate desde, @Param("corte") LocalDate corte);

    @Query("""
           SELECT a.estudiante.id, COUNT(a)
           FROM Attendance a
           WHERE a.estudiante.id IN :ids
             AND a.estado IN ('PRESENTE', 'TARDE')
             AND a.sesion.fecha BETWEEN :desde AND :hasta
           GROUP BY a.estudiante.id
           """)
    List<Object[]> presencesInWindow(@Param("ids") List<Long> ids,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

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
