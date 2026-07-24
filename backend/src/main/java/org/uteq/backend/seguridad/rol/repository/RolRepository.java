package org.uteq.backend.seguridad.rol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.rol.entity.Rol;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);
}
