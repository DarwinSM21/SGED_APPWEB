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

    Page<Asistencia> findByEstudiante_IdEstudianteOrderBySesion_FechaDesc(Long idEstudiante, Pageable pageable);

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

    @Procedure(procedureName = "deportivo.sp_validar_categoria_estudiante_sesion")
    Boolean validarCategoriaCoincide(@Param("p_estudiante") Long idEstudiante, @Param("p_sesion") Long idSesion);

    @Procedure(procedureName = "deportivo.sp_reporte_asistencia_estudiante")
    BigDecimal calcularPorcentajeAsistencia(
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
    List<Object[]> resumenAsistenciaDeActivos(
            @Param("desde") LocalDate desde, @Param("corte") LocalDate corte);

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
