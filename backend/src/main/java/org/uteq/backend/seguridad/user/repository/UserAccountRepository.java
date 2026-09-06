package org.uteq.backend.seguridad.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<UserAccount> findByIdUsuarioAndActivoTrue(Long idUsuario);
    Page<UserAccount> findByActivoTrue(Pageable pageable);

    Optional<UserAccount> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<UserAccount> findByUsernameAndActivoTrue(String username);

    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<UserAccount> findByUsernameIgnoreCaseAndActivoTrue(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByPersona_IdPersona(Long idPersona);

    @EntityGraph(attributePaths = {"roles"})
    Optional<UserAccount> findByPersona_IdPersonaAndActivoTrue(Long idPersona);
}
