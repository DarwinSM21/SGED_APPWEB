package org.uteq.backend.seguridad.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @EntityGraph(attributePaths = {"roles", "person"})
    @Query("SELECT u FROM UserAccount u WHERE u.id = :idUsuario AND u.active = true")
    Optional<UserAccount> findByIdAndActiveTrue(@Param("idUsuario") Long idUsuario);

    /**
     * @param pageable página y tamaño solicitados
     * @return página de cuentas con baja lógica excluida
     */
    @Query("SELECT u FROM UserAccount u WHERE u.active = true")
    Page<UserAccount> findByActiveTrue(Pageable pageable);

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
    @EntityGraph(attributePaths = {"roles", "person"})
    @Query("SELECT u FROM UserAccount u WHERE u.username = :username AND u.active = true")
    Optional<UserAccount> findByUsernameAndActiveTrue(@Param("username") String username);

    /**
     * Igual que {@link #findByUsernameAndActiveTrue(String)} pero sin
     * distinguir mayúsculas/minúsculas en el nombre de usuario.
     *
     * @param username nombre de usuario, sin distinguir mayúsculas/minúsculas
     * @return la cuenta activa con ese nombre de usuario, si existe
     */
    @EntityGraph(attributePaths = {"roles", "person"})
    @Query("SELECT u FROM UserAccount u WHERE LOWER(u.username) = LOWER(:username) AND u.active = true")
    Optional<UserAccount> findByUsernameIgnoreCaseAndActiveTrue(@Param("username") String username);

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
    @Query("SELECT COUNT(u) > 0 FROM UserAccount u WHERE u.person.id = :idPersona")
    boolean existsByPerson_Id(@Param("idPersona") Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return la cuenta activa de esa persona, con roles precargados, si existe
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM UserAccount u WHERE u.person.id = :idPersona AND u.active = true")
    Optional<UserAccount> findByPerson_IdAndActiveTrue(@Param("idPersona") Long idPersona);
}
