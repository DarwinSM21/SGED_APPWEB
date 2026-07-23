package org.uteq.backend.seguridad.auth.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.auth.entity.UsuarioAuth;

import java.util.Optional;

public interface UsuarioAuthRepository extends JpaRepository<UsuarioAuth, Long> {

    @EntityGraph(attributePaths = {"persona", "roles"})
    Optional<UsuarioAuth> findByUsername(String username);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"persona", "roles"})
    Optional<UsuarioAuth> findByUsernameAndActivoTrue(String username);
}
