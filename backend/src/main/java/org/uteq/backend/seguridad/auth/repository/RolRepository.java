package org.uteq.backend.seguridad.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.auth.entity.Rol;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);
}
