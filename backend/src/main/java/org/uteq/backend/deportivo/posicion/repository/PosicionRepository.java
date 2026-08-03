package org.uteq.backend.deportivo.posicion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.posicion.entity.Posicion;

import java.util.List;

public interface PosicionRepository extends JpaRepository<Posicion, Long> {

    List<Posicion> findByActivoTrueOrderByIdPosicionAsc();
}
