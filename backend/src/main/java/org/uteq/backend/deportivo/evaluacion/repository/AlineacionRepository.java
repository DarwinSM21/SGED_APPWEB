package org.uteq.backend.deportivo.evaluacion.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;

import java.util.List;
import java.util.Optional;

public interface AlineacionRepository extends JpaRepository<Alineacion, Long> {

    /**
     * La alineacion guardada de un partido, si la hay. El EntityGraph evita el
     * N+1 al pintar el once: sin el, cada jugador dispara su propia consulta
     * de estudiante, persona y posicion.
     */
    @EntityGraph(attributePaths = {
            "jugadores", "jugadores.estudiante", "jugadores.estudiante.persona", "jugadores.posicion"})
    Optional<Alineacion> findByPartido_IdPartido(Long idPartido);

    boolean existsByPartido_IdPartido(Long idPartido);

    /**
     * Cuantos titulares tiene guardado cada partido de la pagina, en una sola
     * consulta. La lista de partidos lo muestra en cada fila; preguntarlo
     * partido por partido seria un N+1 proporcional al tamano de pagina.
     */
    @Query("""
           SELECT a.partido.idPartido, SUM(CASE WHEN j.titular = true THEN 1L ELSE 0L END)
           FROM Alineacion a
           LEFT JOIN a.jugadores j
           WHERE a.partido.idPartido IN :ids
           GROUP BY a.partido.idPartido
           """)
    List<Object[]> contarTitularesPorPartido(@Param("ids") List<Long> ids);
}
