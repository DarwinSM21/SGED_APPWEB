package org.uteq.backend.seguridad.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.util.Optional;

/**
 * Acceso a las cuentas de acceso (usuario/contraseña) vinculadas a una
 * persona.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /**
     * @param idUsuario identificador de la cuenta
     * @return la cuenta activa, con roles y persona precargados, si existe
     */
    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<UserAccount> findByIdUsuarioAndActivoTrue(Long idUsuario);

    /**
     * @param pageable página y tamaño solicitados
     * @return página de cuentas con baja lógica excluida
     */
    Page<UserAccount> findByActivoTrue(Pageable pageable);

    /**
     * @param username nombre de usuario
     * @return la cuenta con ese nombre de usuario, activa o no, si existe
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Usado en el flujo de autenticación: busca la cuenta activa con sus
     * roles y persona precargados para no disparar consultas adicionales
     * al construir el token.
     *
     * @param username nombre de usuario
     * @return la cuenta activa con ese nombre de usuario, si existe
     */
    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<UserAccount> findByUsernameAndActivoTrue(String username);

    /**
     * Igual que {@link #findByUsernameAndActivoTrue(String)} pero sin
     * distinguir mayúsculas/minúsculas en el nombre de usuario.
     *
     * @param username nombre de usuario, sin distinguir mayúsculas/minúsculas
     * @return la cuenta activa con ese nombre de usuario, si existe
     */
    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<UserAccount> findByUsernameIgnoreCaseAndActivoTrue(String username);

    /**
     * @param username nombre de usuario a comprobar
     * @return {@code true} si ya existe una cuenta con ese nombre de usuario
     */
    boolean existsByUsername(String username);

    /**
     * @param username nombre de usuario a comprobar, sin distinguir mayúsculas/minúsculas
     * @return {@code true} si ya existe una cuenta con ese nombre de usuario
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona ya tiene una cuenta de acceso, activa o no
     */
    boolean existsByPersona_IdPersona(Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return la cuenta activa de esa persona, con roles precargados, si existe
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<UserAccount> findByPersona_IdPersonaAndActivoTrue(Long idPersona);
}
