package org.uteq.backend.deportivo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.deportivo.entity.Posicion;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosicionRepository extends JpaRepository<Posicion, Long> {

    List<Posicion> findByActivoTrue();

    Optional<Posicion> findByNombre(String nombre);
}
