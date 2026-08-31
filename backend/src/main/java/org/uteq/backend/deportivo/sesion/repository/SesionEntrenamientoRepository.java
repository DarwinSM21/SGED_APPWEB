package org.uteq.backend.deportivo.sesion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;

import java.time.LocalDate;
import java.util.List;

public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Long> {
    List<SesionEntrenamiento> findByFechaOrderByHoraInicioAsc(LocalDate fecha);

    @Query("""
           SELECT s FROM SesionEntrenamiento s
            WHERE s.entrenador.idEntrenador = :idEntrenador
            ORDER BY CASE WHEN s.fecha >= CURRENT_DATE THEN 0 ELSE 1 END ASC,
                     CASE WHEN s.fecha >= CURRENT_DATE THEN s.fecha END ASC,
                     s.fecha DESC,
                     s.horaInicio ASC
           """)
    Page<SesionEntrenamiento> sesionesDelEntrenador(@Param("idEntrenador") Long idEntrenador,
                                                    Pageable pageable);

    List<SesionEntrenamiento> findByCategoriaIdCategoriaAndFechaLessThanOrderByFechaDesc(
            Long idCategoria, LocalDate fecha, Pageable pageable);

    boolean existsByHorario_IdHorarioAndFecha(Long idHorario, LocalDate fecha);

    List<SesionEntrenamiento> findByHorario_IdHorarioAndFechaGreaterThanEqual(Long idHorario, LocalDate desde);

    List<SesionEntrenamiento> findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
            Long idCategoria, LocalDate fecha, Pageable pageable);

    @Query("""
           SELECT s.fecha,
                  SUM(CASE WHEN a.estado IN ('PRESENTE', 'TARDE') THEN 1L ELSE 0L END),
                  (SELECT COUNT(e) FROM Estudiante e
                     WHERE e.categoria = s.categoria AND e.activo = true)
           FROM SesionEntrenamiento s
           LEFT JOIN Asistencia a ON a.sesion = s
           WHERE s.fecha BETWEEN :desde AND :hasta
           GROUP BY s.fecha, s.categoria
           ORDER BY s.fecha
           """)
    List<Object[]> resumenAsistenciaPorDia(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
           SELECT COUNT(s) > 0 FROM SesionEntrenamiento s
            WHERE s.categoria.idCategoria = :idCategoria
              AND s.fecha = :fecha
              AND s.horaInicio < :horaFin
              AND s.horaFin > :horaInicio
           """)
    boolean existeSolape(@Param("idCategoria") Long idCategoria,
                         @Param("fecha") LocalDate fecha,
                         @Param("horaInicio") java.time.LocalTime horaInicio,
                         @Param("horaFin") java.time.LocalTime horaFin);

    long countByCategoriaIdCategoriaAndFechaBetween(Long idCategoria, LocalDate desde, LocalDate hasta);
}
