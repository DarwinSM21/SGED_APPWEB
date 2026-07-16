package org.uteq.backend.deportivo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.entity.Posicion;

import java.util.List;
import java.util.Optional;

public interface PosicionRepository extends JpaRepository<Posicion, Long> {

    List<Posicion> findByActivoTrue();

    Optional<Posicion> findByNombre(String nombre);
}
