package org.uteq.backend.inventario.movement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.inventario.movement.entity.StockMovement;

/**
 * Acceso al historial de movimientos de stock (entradas y salidas) de los
 * artículos de inventario.
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * @param pageable página y tamaño solicitados
     * @return todos los movimientos, del más reciente al más antiguo
     */
    @Query("SELECT m FROM StockMovement m ORDER BY m.movementDate DESC")
    Page<StockMovement> findAllByOrderByFechaMovimientoDesc(Pageable pageable);

    /**
     * @param idArticulo identificador del artículo
     * @param pageable página y tamaño solicitados
     * @return los movimientos de ese artículo, del más reciente al más antiguo
     */
    @Query("SELECT m FROM StockMovement m WHERE m.item.id = :idArticulo ORDER BY m.movementDate DESC")
    Page<StockMovement> findByArticulo_IdArticuloOrderByFechaMovimientoDesc(@Param("idArticulo") Long idArticulo, Pageable pageable);
}
