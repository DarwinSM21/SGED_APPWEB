package org.uteq.backend.deportivo.position.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.uteq.backend.deportivo.position.entity.Position;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {

    @Query("SELECT p FROM Position p WHERE p.activo = true ORDER BY p.idPosicion ASC")
    List<Position> findActiveOrderByIdAsc();
}
