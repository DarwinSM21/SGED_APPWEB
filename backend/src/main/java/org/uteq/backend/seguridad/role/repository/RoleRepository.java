package org.uteq.backend.seguridad.role.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.seguridad.role.entity.Role;

import java.util.Optional;

/**
 * Acceso al catálogo de roles del sistema (ADMINISTRADOR, ENTRENADOR, etc.).
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * @param nombre nombre del rol
     * @return el rol con ese nombre, si existe
     */
    @Query("SELECT r FROM Role r WHERE r.name = :nombre")
    Optional<Role> findByNombre(@Param("nombre") String nombre);
}
