package org.uteq.backend.seguridad.role.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.role.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNombre(String nombre);
}
