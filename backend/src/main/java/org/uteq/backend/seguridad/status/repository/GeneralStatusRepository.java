package org.uteq.backend.seguridad.status.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;

import java.util.Optional;

public interface GeneralStatusRepository extends JpaRepository<GeneralStatus, Long> {

    Optional<GeneralStatus> findByNombre(String nombre);

    boolean existsByNombre(String nombre);


}
