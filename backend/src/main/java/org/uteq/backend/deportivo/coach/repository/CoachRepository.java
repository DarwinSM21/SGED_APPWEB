package org.uteq.backend.deportivo.coach.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.coach.entity.Coach;

import java.util.Optional;

/**
 * Acceso a las fichas de entrenador, incluida la comprobación de duplicados
 * de vínculo con persona/usuario.
 */
public interface CoachRepository extends JpaRepository<Coach, Long> {
    /**
     * @param pageable página y tamaño solicitados
     * @return página de entrenadores activos
     */
    @Query("SELECT e FROM Coach e WHERE e.activo = true")
    Page<Coach> findActiveTrue(Pageable pageable);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona ya tiene una ficha de entrenador, activa o no
     */
    @Query("SELECT COUNT(e) > 0 FROM Coach e WHERE e.persona.id = :idPersona")
    boolean existsByPerson_Id(@Param("idPersona") Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona tiene una ficha de entrenador activa
     */
    @Query("SELECT COUNT(e) > 0 FROM Coach e WHERE e.persona.id = :idPersona AND e.activo = true")
    boolean existsByPerson_IdAndActiveTrue(@Param("idPersona") Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return la ficha de entrenador activa de esa persona, si existe
     */
    @Query("SELECT e FROM Coach e WHERE e.persona.id = :idPersona AND e.activo = true")
    Optional<Coach> findByPerson_IdAndActiveTrue(@Param("idPersona") Long idPersona);

    /**
     * @param idUsuario identificador de la cuenta de usuario
     * @return {@code true} si esa cuenta está vinculada a una ficha de entrenador
     */
    @Query("SELECT COUNT(e) > 0 FROM Coach e WHERE e.usuario.id = :idUsuario")
    boolean existsByUserAccount_Id(@Param("idUsuario") Long idUsuario);

    /**
     * @param username nombre de usuario de la cuenta de acceso
     * @return el entrenador cuya cuenta tiene ese nombre de usuario, si existe
     */
    @Query("SELECT e FROM Coach e WHERE e.usuario.username = :username")
    Optional<Coach> findByUserAccount_Username(@Param("username") String username);
}
