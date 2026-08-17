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
}
