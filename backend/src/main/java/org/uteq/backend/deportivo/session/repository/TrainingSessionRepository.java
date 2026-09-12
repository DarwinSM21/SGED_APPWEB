package org.uteq.backend.deportivo.session.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.session.entity.TrainingSession;

import java.time.LocalDate;
import java.util.List;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    @Query("SELECT s FROM TrainingSession s WHERE s.fecha = :fecha ORDER BY s.horaInicio ASC")
    List<TrainingSession> findByDateOrderByStartTimeAsc(@Param("fecha") LocalDate fecha);

    @Query("""
           SELECT s FROM TrainingSession s
            WHERE s.entrenador.idEntrenador = :idEntrenador
            ORDER BY CASE WHEN s.fecha >= CURRENT_DATE THEN 0 ELSE 1 END ASC,
                     CASE WHEN s.fecha >= CURRENT_DATE THEN s.fecha END ASC,
                     s.fecha DESC,
                     s.horaInicio ASC
           """)
    Page<TrainingSession> sessionsByCoach(@Param("idEntrenador") Long idEntrenador,
                                                    Pageable pageable);

    @Query("SELECT s FROM TrainingSession s WHERE s.categoria.idCategoria = :idCategoria AND s.fecha < :fecha ORDER BY s.fecha DESC")
    List<TrainingSession> findByCategoryAndDateBeforeOrderByDateDesc(
            @Param("idCategoria") Long idCategoria, @Param("fecha") LocalDate fecha, Pageable pageable);

    @Query("SELECT COUNT(s) > 0 FROM TrainingSession s WHERE s.horario.idHorario = :idHorario AND s.fecha = :fecha")
    boolean existsBySchedule_IdAndDate(@Param("idHorario") Long idHorario, @Param("fecha") LocalDate fecha);

    @Query("SELECT s FROM TrainingSession s WHERE s.horario.idHorario = :idHorario AND s.fecha >= :desde")
    List<TrainingSession> findBySchedule_IdAndDateGreaterThanEqual(@Param("idHorario") Long idHorario, @Param("desde") LocalDate desde);

    @Query("SELECT s FROM TrainingSession s WHERE s.categoria.idCategoria = :idCategoria AND s.fecha >= :fecha ORDER BY s.fecha ASC, s.horaInicio ASC")
    List<TrainingSession> findByCategoryAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            @Param("idCategoria") Long idCategoria, @Param("fecha") LocalDate fecha, Pageable pageable);

    @Query("""
           SELECT s.fecha,
                  SUM(CASE WHEN a.estado IN ('PRESENTE', 'TARDE') THEN 1L ELSE 0L END),
                  (SELECT COUNT(e) FROM Student e
                     WHERE e.category = s.categoria AND e.active = true)
           FROM TrainingSession s
           LEFT JOIN Attendance a ON a.sesion = s
           WHERE s.fecha BETWEEN :desde AND :hasta
           GROUP BY s.fecha, s.categoria
           ORDER BY s.fecha
           """)
    List<Object[]> attendanceSummaryByDay(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
           SELECT COUNT(s) > 0 FROM TrainingSession s
            WHERE s.categoria.idCategoria = :idCategoria
              AND s.fecha = :fecha
              AND s.horaInicio < :horaFin
              AND s.horaFin > :horaInicio
           """)
    boolean hasOverlap(@Param("idCategoria") Long idCategoria,
                         @Param("fecha") LocalDate fecha,
                         @Param("horaInicio") java.time.LocalTime horaInicio,
                         @Param("horaFin") java.time.LocalTime horaFin);

    @Query("SELECT COUNT(s) FROM TrainingSession s WHERE s.categoria.idCategoria = :idCategoria AND s.fecha BETWEEN :desde AND :hasta")
    long countByCategoryAndDateBetween(@Param("idCategoria") Long idCategoria, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
