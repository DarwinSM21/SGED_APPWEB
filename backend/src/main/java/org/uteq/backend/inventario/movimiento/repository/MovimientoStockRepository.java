package org.uteq.backend.inventario.movimiento.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.inventario.movimiento.entity.MovimientoStock;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    Page<MovimientoStock> findAllByOrderByFechaMovimientoDesc(Pageable pageable);

    Page<MovimientoStock> findByArticulo_IdArticuloOrderByFechaMovimientoDesc(Long idArticulo, Pageable pageable);
}
