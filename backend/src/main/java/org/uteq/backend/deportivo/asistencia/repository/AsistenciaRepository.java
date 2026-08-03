package org.uteq.backend.deportivo.asistencia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    Optional<Asistencia> findBySesionIdSesionAndEstudianteIdEstudiante(Long idSesion, Long idEstudiante);

    List<Asistencia> findBySesionIdSesion(Long idSesion);

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
}
