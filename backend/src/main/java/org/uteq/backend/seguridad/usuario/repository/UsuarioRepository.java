package org.uteq.backend.seguridad.usuario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<Usuario> findByIdUsuarioAndActivoTrue(Long idUsuario);
    Page<Usuario> findByActivoTrue(Pageable pageable);

    Optional<Usuario> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<Usuario> findByUsernameAndActivoTrue(String username);

    boolean existsByUsername(String username);

    boolean existsByPersona_IdPersona(Long idPersona);
}