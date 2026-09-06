package org.uteq.backend.inventario.item.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.uteq.backend.inventario.item.entity.Item;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByActivoTrue(Pageable pageable);

    List<Item> findByActivoTrue();

    @Query("SELECT a FROM Item a WHERE a.activo = true AND a.stockActual <= a.stockMinimo")
    List<Item> findLowStock();

    @Procedure(procedureName = "inventario.sp_reporte_stock_bajo")
    Long countLowStock();
}
