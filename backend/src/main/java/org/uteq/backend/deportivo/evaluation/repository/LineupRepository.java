package org.uteq.backend.deportivo.evaluation.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluation.entity.Lineup;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la plantilla (alineación sugerida o guardada) de un partido, con
 * los jugadores convocados y su posición.
 */
public interface LineupRepository extends JpaRepository<Lineup, Long> {
    /**
     * @param idPartido identificador del partido
     * @return la plantilla de ese partido, con jugadores/persona/posición precargados, si existe
     */
    @EntityGraph(attributePaths = {
            "jugadores", "jugadores.estudiante", "jugadores.estudiante.person", "jugadores.posicion"})
    @Query("SELECT a FROM Lineup a WHERE a.partido.idPartido = :idPartido")
    Optional<Lineup> findByMatch_Id(@Param("idPartido") Long idPartido);

    /**
     * @param idPartido identificador del partido
     * @return {@code true} si ese partido ya tiene una plantilla guardada
     */
    @Query("SELECT COUNT(a) > 0 FROM Lineup a WHERE a.partido.idPartido = :idPartido")
    boolean existsByMatch_Id(@Param("idPartido") Long idPartido);

    /**
     * @param ids identificadores de los partidos a considerar
     * @return filas {@code [idPartido, cantidad de titulares]} de las plantillas de esos partidos
     */
    @Query("""
           SELECT a.partido.idPartido, SUM(CASE WHEN j.titular = true THEN 1L ELSE 0L END)
           FROM Lineup a
           LEFT JOIN a.jugadores j
           WHERE a.partido.idPartido IN :ids
           GROUP BY a.partido.idPartido
           """)
    List<Object[]> countStartersByMatch(@Param("ids") List<Long> ids);
}
