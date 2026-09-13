package org.uteq.backend.deportivo.session.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.session.entity.TrainingSession;

import java.time.LocalDate;
import java.util.List;

/**
 * Acceso a las sesiones de entrenamiento concretas (generadas a partir de un
 * horario fijo, o creadas sueltas), incluidos los resúmenes agregados de
 * asistencia y las comprobaciones de solapamiento usadas al programar.
 */
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    /**
     * @param fecha fecha a filtrar
     * @return las sesiones de esa fecha, ordenadas por hora de inicio
     */
    @Query("SELECT s FROM TrainingSession s WHERE s.fecha = :fecha ORDER BY s.horaInicio ASC")
    List<TrainingSession> findByDateOrderByStartTimeAsc(@Param("fecha") LocalDate fecha);

    /**
     * @param idEntrenador identificador del entrenador
     * @param pageable página y tamaño solicitados
     * @return las sesiones de ese entrenador, primero las próximas (de la más
     *         cercana a la más lejana) y luego las pasadas (de la más
     *         reciente a la más antigua)
     */
    @Query("""
           SELECT s FROM TrainingSession s
            WHERE s.entrenador.coachId = :idEntrenador
            ORDER BY CASE WHEN s.fecha >= CURRENT_DATE THEN 0 ELSE 1 END ASC,
                     CASE WHEN s.fecha >= CURRENT_DATE THEN s.fecha END ASC,
                     s.fecha DESC,
                     s.horaInicio ASC
           """)
    Page<TrainingSession> sessionsByCoach(@Param("idEntrenador") Long idEntrenador,
                                                    Pageable pageable);

    /**
     * @param idCategoria identificador de la categoría
     * @param fecha fecha límite, exclusiva
     * @param pageable página y tamaño solicitados
     * @return las sesiones de esa categoría anteriores a esa fecha, de la más reciente a la más antigua
     */
    @Query("SELECT s FROM TrainingSession s WHERE s.categoria.categoryId = :idCategoria AND s.fecha < :fecha ORDER BY s.fecha DESC")
    List<TrainingSession> findByCategoryAndDateBeforeOrderByDateDesc(
            @Param("idCategoria") Long idCategoria, @Param("fecha") LocalDate fecha, Pageable pageable);

    /**
     * @param idHorario identificador del horario fijo
     * @param fecha fecha a comprobar
     * @return {@code true} si ya existe una sesión generada de ese horario para esa fecha
     */
    @Query("SELECT COUNT(s) > 0 FROM TrainingSession s WHERE s.horario.idHorario = :idHorario AND s.fecha = :fecha")
    boolean existsBySchedule_IdAndDate(@Param("idHorario") Long idHorario, @Param("fecha") LocalDate fecha);

    /**
     * @param idHorario identificador del horario fijo
     * @param desde fecha desde la que se buscan sesiones, inclusive
     * @return las sesiones generadas de ese horario desde esa fecha
     */
    @Query("SELECT s FROM TrainingSession s WHERE s.horario.idHorario = :idHorario AND s.fecha >= :desde")
    List<TrainingSession> findBySchedule_IdAndDateGreaterThanEqual(@Param("idHorario") Long idHorario, @Param("desde") LocalDate desde);

    /**
     * @param idCategoria identificador de la categoría
     * @param fecha fecha desde la que se buscan sesiones, inclusive
     * @param pageable página y tamaño solicitados
     * @return las sesiones de esa categoría desde esa fecha, de la más próxima a la más lejana
     */
    @Query("SELECT s FROM TrainingSession s WHERE s.categoria.categoryId = :idCategoria AND s.fecha >= :fecha ORDER BY s.fecha ASC, s.horaInicio ASC")
    List<TrainingSession> findByCategoryAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            @Param("idCategoria") Long idCategoria, @Param("fecha") LocalDate fecha, Pageable pageable);

    /**
     * Resumen diario de asistencia por sesión, contrastado contra el total
     * de estudiantes activos de la categoría (para el mapa de calor del
     * panel administrativo).
     *
     * @param desde fecha inicial del rango, inclusive
     * @param hasta fecha final del rango, inclusive
     * @return filas {@code [fecha, presencias, total de estudiantes activos de la categoría]} por sesión
     */
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

    /**
     * Comprueba si ya existe una sesión de la misma categoría que se cruza
     * con un rango horario dado en una fecha, para rechazar sesiones sueltas
     * que chocarían entre sí.
     *
     * @param idCategoria identificador de la categoría
     * @param fecha fecha a comprobar
     * @param horaInicio hora de inicio del rango propuesto
     * @param horaFin hora de fin del rango propuesto
     * @return {@code true} si existe una sesión de esa categoría y fecha que se cruza con el rango propuesto
     */
    @Query("""
           SELECT COUNT(s) > 0 FROM TrainingSession s
            WHERE s.categoria.categoryId = :idCategoria
              AND s.fecha = :fecha
              AND s.horaInicio < :horaFin
              AND s.horaFin > :horaInicio
           """)
    boolean hasOverlap(@Param("idCategoria") Long idCategoria,
                         @Param("fecha") LocalDate fecha,
                         @Param("horaInicio") java.time.LocalTime horaInicio,
                         @Param("horaFin") java.time.LocalTime horaFin);

    /**
     * @param idCategoria identificador de la categoría
     * @param desde fecha inicial del rango, inclusive
     * @param hasta fecha final del rango, inclusive
     * @return la cantidad de sesiones de esa categoría en ese rango de fechas
     */
    @Query("SELECT COUNT(s) FROM TrainingSession s WHERE s.categoria.categoryId = :idCategoria AND s.fecha BETWEEN :desde AND :hasta")
    long countByCategoryAndDateBetween(@Param("idCategoria") Long idCategoria, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
