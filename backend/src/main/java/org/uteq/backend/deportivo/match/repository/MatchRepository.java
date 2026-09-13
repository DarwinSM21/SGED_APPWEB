package org.uteq.backend.deportivo.match.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.match.entity.Match;

import java.util.Optional;

/**
 * Acceso al calendario de partidos, con la categoría precargada para evitar
 * N+1 en las listas.
 */
public interface MatchRepository extends JpaRepository<Match, Long> {
    /**
     * @param pageable página y tamaño solicitados
     * @return página de partidos, del más reciente al más antiguo
     */
    @EntityGraph(attributePaths = "categoria")
    @Query("SELECT p FROM Match p ORDER BY p.fecha DESC, p.hora DESC")
    Page<Match> findAllOrderByDateDescTimeDesc(Pageable pageable);

    /**
     * @param idCategoria identificador de la categoría a filtrar
     * @param pageable página y tamaño solicitados
     * @return página de partidos de esa categoría, del más reciente al más antiguo
     */
    @EntityGraph(attributePaths = "categoria")
    @Query("SELECT p FROM Match p WHERE p.categoria.categoryId = :idCategoria ORDER BY p.fecha DESC, p.hora DESC")
    Page<Match> findByCategoryOrderByDateDescTimeDesc(@Param("idCategoria") Long idCategoria, Pageable pageable);

    /**
     * @param idPartido identificador del partido
     * @return el partido con su categoría precargada, si existe
     */
    @EntityGraph(attributePaths = "categoria")
    @Query("SELECT p FROM Match p WHERE p.idPartido = :idPartido")
    Optional<Match> findWithCategoryById(@Param("idPartido") Long idPartido);
}
