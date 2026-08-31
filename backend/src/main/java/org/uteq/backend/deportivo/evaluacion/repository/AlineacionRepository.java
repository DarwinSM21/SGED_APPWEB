package org.uteq.backend.deportivo.evaluacion.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;

import java.util.List;
import java.util.Optional;

public interface AlineacionRepository extends JpaRepository<Alineacion, Long> {
    @EntityGraph(attributePaths = {
            "jugadores", "jugadores.estudiante", "jugadores.estudiante.persona", "jugadores.posicion"})
    Optional<Alineacion> findByPartido_IdPartido(Long idPartido);

    boolean existsByPartido_IdPartido(Long idPartido);

    @Query("""
           SELECT a.partido.idPartido, SUM(CASE WHEN j.titular = true THEN 1L ELSE 0L END)
           FROM Alineacion a
           LEFT JOIN a.jugadores j
           WHERE a.partido.idPartido IN :ids
           GROUP BY a.partido.idPartido
           """)
    List<Object[]> contarTitularesPorPartido(@Param("ids") List<Long> ids);
}
