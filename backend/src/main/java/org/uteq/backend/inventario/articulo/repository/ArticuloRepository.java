package org.uteq.backend.inventario.articulo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.uteq.backend.inventario.articulo.entity.Articulo;

import java.util.List;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    Page<Articulo> findByActivoTrue(Pageable pageable);

    List<Articulo> findByActivoTrue();

    @Query("SELECT a FROM Articulo a WHERE a.activo = true AND a.stockActual <= a.stockMinimo")
    List<Articulo> findConStockBajo();

    @Procedure(procedureName = "inventario.sp_reporte_stock_bajo")
    Long contarStockBajo();
}
