package org.uteq.backend.deportivo.category.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.category.entity.Category;

import java.util.List;

/**
 * Acceso al catálogo de categorías deportivas (ej. SUB-12, SUB-14).
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * @param pageable página y tamaño solicitados
     * @return página de categorías activas
     */
    @Query("SELECT c FROM Category c WHERE c.activo = true")
    Page<Category> findActiveTrue(Pageable pageable);

    /**
     * @return todas las categorías activas, sin paginar
     */
    @Query("SELECT c FROM Category c WHERE c.activo = true")
    List<Category> findActiveTrue();

    /**
     * @param nombre nombre de la categoría a comprobar
     * @return {@code true} si ya existe una categoría con ese nombre, sin distinguir mayúsculas/minúsculas
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.nombre) = LOWER(:nombre)")
    boolean existsByNameIgnoreCase(@Param("nombre") String nombre);

    /**
     * Comprueba unicidad del nombre excluyendo a la propia categoría, para
     * permitir editarla sin chocar consigo misma.
     *
     * @param nombre nombre a comprobar
     * @param idCategoria identificador de la categoría que se excluye de la comprobación
     * @return {@code true} si otra categoría ya usa ese nombre
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.nombre) = LOWER(:nombre) AND c.categoryId <> :idCategoria")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("idCategoria") Long idCategoria);
}
