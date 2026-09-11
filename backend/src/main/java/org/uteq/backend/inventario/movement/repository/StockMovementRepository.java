package org.uteq.backend.inventario.movement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
    Page<StockMovement> findAllByOrderByFechaMovimientoDesc(Pageable pageable);

    /**
     * @param idArticulo identificador del artículo
     * @param pageable página y tamaño solicitados
     * @return los movimientos de ese artículo, del más reciente al más antiguo
     */
    Page<StockMovement> findByArticulo_IdArticuloOrderByFechaMovimientoDesc(Long idArticulo, Pageable pageable);
}
