package org.uteq.backend.inventario.item.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.uteq.backend.inventario.item.entity.Item;

import java.util.List;

/**
 * Acceso al catálogo de artículos de inventario y a la consulta de stock bajo.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * @param pageable página y tamaño solicitados
     * @return página de artículos con baja lógica excluida
     */
    @Query("SELECT a FROM Item a WHERE a.active = true")
    Page<Item> findByActiveTrue(Pageable pageable);

    /**
     * @return todos los artículos activos, sin paginar
     */
    @Query("SELECT a FROM Item a WHERE a.active = true")
    List<Item> findByActiveTrue();

    /**
     * @return artículos activos cuyo stock actual llegó o quedó por debajo del mínimo configurado
     */
    @Query("SELECT a FROM Item a WHERE a.active = true AND a.currentStock <= a.minimumStock")
    List<Item> findLowStock();

    /**
     * @return la cantidad de artículos en stock bajo, calculada por el procedimiento almacenado
     */
    @Procedure(procedureName = "inventario.sp_reporte_stock_bajo")
    Long countLowStock();
}
