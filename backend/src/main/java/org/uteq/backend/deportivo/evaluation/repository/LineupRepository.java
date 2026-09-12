package org.uteq.backend.deportivo.evaluation.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluation.entity.Lineup;

import java.util.List;
import java.util.Optional;

public interface LineupRepository extends JpaRepository<Lineup, Long> {
    @EntityGraph(attributePaths = {
            "jugadores", "jugadores.estudiante", "jugadores.estudiante.person", "jugadores.posicion"})
    @Query("SELECT a FROM Lineup a WHERE a.partido.idPartido = :idPartido")
    Optional<Lineup> findByMatch_Id(@Param("idPartido") Long idPartido);

    @Query("SELECT COUNT(a) > 0 FROM Lineup a WHERE a.partido.idPartido = :idPartido")
    boolean existsByMatch_Id(@Param("idPartido") Long idPartido);

    @Query("""
           SELECT a.partido.idPartido, SUM(CASE WHEN j.titular = true THEN 1L ELSE 0L END)
           FROM Lineup a
           LEFT JOIN a.jugadores j
           WHERE a.partido.idPartido IN :ids
           GROUP BY a.partido.idPartido
           """)
    List<Object[]> countStartersByMatch(@Param("ids") List<Long> ids);
}
