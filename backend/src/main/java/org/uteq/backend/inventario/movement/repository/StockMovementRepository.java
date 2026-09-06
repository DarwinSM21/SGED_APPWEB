package org.uteq.backend.inventario.movement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.inventario.movement.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findAllByOrderByFechaMovimientoDesc(Pageable pageable);

    Page<StockMovement> findByArticulo_IdArticuloOrderByFechaMovimientoDesc(Long idArticulo, Pageable pageable);
}
