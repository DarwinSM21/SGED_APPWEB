package org.uteq.backend.seguridad.estado.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;

import java.util.Optional;

public interface EstadoGeneralRepository extends JpaRepository<EstadoGeneral, Long> {

    Optional<EstadoGeneral> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}