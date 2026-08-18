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

    /**
     * Login sin distinguir mayusculas. Los usernames son correos, que por
     * convencion no son sensibles a mayusculas, y los teclados de celular
     * capitalizan la primera letra sola: sin esto, escribir
     * "Juan.perez@sged.test" en un telefono daba 401 y en la auditoria
     * quedaba como intento fallido, sin ninguna pista de por que.
     */
    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<Usuario> findByUsernameIgnoreCaseAndActivoTrue(String username);

    boolean existsByUsername(String username);

    /** Alta: dos cuentas que solo difieran en mayusculas serian indistinguibles al iniciar sesion. */
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByPersona_IdPersona(Long idPersona);

    /** Coherencia rol-ficha vista desde el otro lado: que rol tiene ya la cuenta de esta persona. */
    @EntityGraph(attributePaths = {"roles"})
    Optional<Usuario> findByPersona_IdPersonaAndActivoTrue(Long idPersona);
}