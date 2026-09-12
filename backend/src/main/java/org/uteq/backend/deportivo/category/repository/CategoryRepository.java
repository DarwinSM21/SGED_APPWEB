package org.uteq.backend.deportivo.category.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.category.entity.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c WHERE c.activo = true")
    Page<Category> findActiveTrue(Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.activo = true")
    List<Category> findActiveTrue();

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.nombre) = LOWER(:nombre)")
    boolean existsByNameIgnoreCase(@Param("nombre") String nombre);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.nombre) = LOWER(:nombre) AND c.idCategoria <> :idCategoria")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("idCategoria") Long idCategoria);
}
