package org.uteq.backend.auth.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.auth.entity.Usuario;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"persona", "roles"})
    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"persona", "roles"})
    Optional<Usuario> findByUsernameAndActivoTrue(String username);
}
