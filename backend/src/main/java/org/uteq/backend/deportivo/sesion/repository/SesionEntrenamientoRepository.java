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

    Page<SesionEntrenamiento> findByEntrenadorIdEntrenadorOrderByFechaDesc(Long idEntrenador, Pageable pageable);

    /**
     * Sesion anterior de la misma categoria. Es la que alimenta la precarga:
     * cada dia arranca con los valores del entrenamiento previo, para que el
     * entrenador ajuste lo que cambio en vez de calificar todo desde cero.
     */
    List<SesionEntrenamiento> findByCategoriaIdCategoriaAndFechaLessThanOrderByFechaDesc(
            Long idCategoria, LocalDate fecha, Pageable pageable);

    /** Idempotencia de HorarioService.generarSesionesProgramadas(): no duplicar la sesion de un horario ya generada hoy. */
    boolean existsByHorario_IdHorarioAndFecha(Long idHorario, LocalDate fecha);

    /**
     * Sesion futura mas proxima de una categoria, para saber "quien es mi
     * entrenador" desde el punto de vista del estudiante. Se usa con
     * PageRequest.of(0, 1): la primera fila es la respuesta, lista vacia
     * significa que no hay ninguna sesion programada todavia.
     */
    List<SesionEntrenamiento> findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
            Long idCategoria, LocalDate fecha, Pageable pageable);

    /**
     * Asistencia agrupada por dia: cuantos se presentaron y cuantos se
     * esperaban. La consulta arranca en la sesion y no en la asistencia
     * porque un dia al que no fue nadie no tiene ni una fila de asistencia,
     * y es justo el dia que hay que ver en el mapa de calor; partiendo de
     * Asistencia ese dia desapareceria del grafico y el hueco se leeria
     * como "no hubo entrenamiento".
     *
     * Devuelve una fila por dia y categoria: si dos categorias entrenan el
     * mismo dia, el servicio las suma.
     */
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
}
