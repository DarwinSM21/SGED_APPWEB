package org.uteq.backend.deportivo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.deportivo.entity.Asistencia;
import org.uteq.backend.deportivo.entity.Asistencia.EstadoAsistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    Optional<Asistencia> findBySesionIdSesionAndEstudianteIdEstudiante(Long idSesion, Long idEstudiante);

    List<Asistencia> findBySesionIdSesion(Long idSesion);

    Page<Asistencia> findByEstudianteIdEstudiante(Long idEstudiante, Pageable pageable);

    @Query("SELECT a FROM Asistencia a WHERE a.sesion.fecha = :fecha AND a.estudiante.idEstudiante = :idEstudiante")
    List<Asistencia> findByFechaAndEstudiante(
            @Param("fecha") LocalDate fecha,
            @Param("idEstudiante") Long idEstudiante);

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.estudiante.idEstudiante = :idEstudiante " +
           "AND a.estado IN ('PRESENTE', 'TARDE')")
    long countPresenciasBy(@Param("idEstudiante") Long idEstudiante);

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.estudiante.idEstudiante = :idEstudiante " +
           "AND a.estado = 'AUSENTE'")
    long countAusenciasBy(@Param("idEstudiante") Long idEstudiante);
}
