package org.uteq.backend.deportivo.partido.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.partido.entity.Partido;

import java.util.Optional;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

    /**
     * El EntityGraph trae la categoria en la misma consulta: la lista la
     * muestra en cada fila y sin el seria un N+1 con {@code open-in-view:
     * false}, que ademas revienta con LazyInitializationException fuera de la
     * transaccion.
     */
    @EntityGraph(attributePaths = "categoria")
    Page<Partido> findAllByOrderByFechaDescHoraDesc(Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Partido> findByCategoria_IdCategoriaOrderByFechaDescHoraDesc(Long idCategoria, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Optional<Partido> findWithCategoriaByIdPartido(Long idPartido);
}
