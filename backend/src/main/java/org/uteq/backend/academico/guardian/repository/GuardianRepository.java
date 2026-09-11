package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.guardian.entity.Guardian;

import java.util.Optional;

/**
 * Acceso a las fichas de representante legal (guardián) de un estudiante.
 */
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    /**
     * Representantes activos, paginados.
     *
     * @param pageable página y tamaño solicitados
     * @return página de representantes con baja lógica excluida
     */
    @Query("SELECT g FROM Guardian g WHERE g.active = true")
    Page<Guardian> findByActivoTrue(Pageable pageable);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona ya tiene una ficha de representante,
     *         activa o no
     */
    @Query("SELECT COUNT(g) > 0 FROM Guardian g WHERE g.person.id = :idPersona")
    boolean existsByPersona_IdPersona(@Param("idPersona") Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona tiene una ficha de representante activa
     */
    @Query("SELECT COUNT(g) > 0 FROM Guardian g WHERE g.person.id = :idPersona AND g.active = true")
    boolean existsByPersona_IdPersonaAndActivoTrue(@Param("idPersona") Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return la ficha de representante activa asociada a esa persona, si existe
     */
    @Query("SELECT g FROM Guardian g WHERE g.person.id = :idPersona AND g.active = true")
    Optional<Guardian> findByPersona_IdPersonaAndActivoTrue(@Param("idPersona") Long idPersona);

    /**
     * @param idUsuario identificador de la cuenta de usuario
     * @return {@code true} si esa cuenta está vinculada a una ficha de representante
     */
    @Query("SELECT COUNT(g) > 0 FROM Guardian g WHERE g.userAccount.id = :idUsuario")
    boolean existsByUsuario_IdUsuario(@Param("idUsuario") Long idUsuario);

    /**
     * @param username nombre de usuario de la cuenta
     * @return el representante cuya cuenta tiene ese nombre de usuario, si existe
     */
    @Query("SELECT g FROM Guardian g WHERE g.userAccount.username = :username")
    Optional<Guardian> findByUsuario_Username(@Param("username") String username);
}
