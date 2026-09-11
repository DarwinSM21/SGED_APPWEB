package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
    Page<Guardian> findByActivoTrue(Pageable pageable);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona ya tiene una ficha de representante,
     *         activa o no
     */
    boolean existsByPersona_IdPersona(Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona tiene una ficha de representante activa
     */
    boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return la ficha de representante activa asociada a esa persona, si existe
     */
    Optional<Guardian> findByPersona_IdPersonaAndActivoTrue(Long idPersona);

    /**
     * @param idUsuario identificador de la cuenta de usuario
     * @return {@code true} si esa cuenta está vinculada a una ficha de representante
     */
    boolean existsByUsuario_IdUsuario(Long idUsuario);

    /**
     * @param username nombre de usuario de la cuenta
     * @return el representante cuya cuenta tiene ese nombre de usuario, si existe
     */
    Optional<Guardian> findByUsuario_Username(String username);
}
