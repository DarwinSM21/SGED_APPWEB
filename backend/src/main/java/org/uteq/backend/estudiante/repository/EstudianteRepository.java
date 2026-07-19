package org.uteq.backend.estudiante.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.estudiante.entity.Estudiante;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Page<Estudiante> findByActivoTrue(Pageable pageable);

    @Query("SELECT COUNT(e) FROM Estudiante e WHERE e.activo = true AND e.categoria = :categoria")
    long contarActivosPorCategoria(@Param("categoria") String categoria);
}
