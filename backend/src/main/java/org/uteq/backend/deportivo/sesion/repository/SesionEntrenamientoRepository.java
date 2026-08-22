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

    /**
     * Sesiones del entrenador con HOY primero.
     *
     * Antes se ordenaba solo por fecha descendente, asi que al abrir la
     * pantalla lo primero que aparecia era la sesion de dentro de una semana
     * -las futuras se generan con siete dias de antelacion- y habia que
     * bajar cinco dias para llegar a la de hoy, que es justamente sobre la
     * que se trabaja.
     *
     * El orden es: hoy y lo que viene primero, de lo mas cercano a lo mas
     * lejano; despues el pasado, de lo mas reciente hacia atras.
     */
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

    /**
     * Sesiones de esa categoria y fecha cuyo horario se cruza con el rango
     * dado. Se compara por SOLAPE y no por hora exacta a proposito: la
     * escuela si puede tener dos sesiones el mismo dia -una en la manana y
     * otra en la tarde-, lo que no puede es tener dos a la misma hora, ni una
     * de 16:00 a 18:00 junto a otra de 17:00 a 19:00, porque el mismo grupo
     * no esta en dos entrenamientos a la vez y la asistencia se repartiria
     * entre ambas sin criterio.
     *
     * Dos rangos se cruzan si cada uno empieza antes de que termine el otro.
     */
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
}
