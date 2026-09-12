package org.uteq.backend.deportivo.match.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.match.entity.Match;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    @EntityGraph(attributePaths = "categoria")
    @Query("SELECT p FROM Match p ORDER BY p.fecha DESC, p.hora DESC")
    Page<Match> findAllOrderByDateDescTimeDesc(Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    @Query("SELECT p FROM Match p WHERE p.categoria.idCategoria = :idCategoria ORDER BY p.fecha DESC, p.hora DESC")
    Page<Match> findByCategoryOrderByDateDescTimeDesc(@Param("idCategoria") Long idCategoria, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    @Query("SELECT p FROM Match p WHERE p.idPartido = :idPartido")
    Optional<Match> findWithCategoryById(@Param("idPartido") Long idPartido);
}
