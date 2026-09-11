package org.uteq.backend.seguridad.person.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acceso a la ficha base de persona, común a todos los roles del sistema.
 */
public interface PersonRepository extends JpaRepository<Person, Long> {

    /**
     * @param pageable página y tamaño solicitados
     * @return página de personas con baja lógica excluida
     */
    Page<Person> findByActivoTrue(Pageable pageable);

    /**
     * @param cedula cédula a buscar (puede no estar presente, ver RF-49)
     * @return la persona activa con esa cédula, si existe
     */
    Optional<Person> findByCedulaAndActivoTrue(String cedula);

    /**
     * @param idPersona identificador de la persona
     * @return la persona, si existe y está activa
     */
    Optional<Person> findByIdPersonaAndActivoTrue(Long idPersona);

    /**
     * @param correo correo electrónico a buscar
     * @return la persona con ese correo, activa o no, si existe
     */
    Optional<Person> findByCorreo(String correo);

    /**
     * @param cedula cédula a comprobar
     * @return {@code true} si existe una persona activa con esa cédula
     */
    boolean existsByCedulaAndActivoTrue(String cedula);

    /**
     * @param correo correo electrónico a comprobar
     * @return {@code true} si ya existe una persona con ese correo
     */
    boolean existsByCorreo(String correo);

    /**
     * Comprueba unicidad de cédula excluyendo a la propia persona, para
     * permitir actualizar una ficha sin chocar consigo misma.
     *
     * @param cedula cédula a comprobar
     * @param idPersona identificador de la persona que se excluye de la comprobación
     * @return {@code true} si otra persona activa ya usa esa cédula
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.cedula = :cedula AND p.activo = true AND p.idPersona != :idPersona")
    boolean existsAnotherPersonWithCedula(@Param("cedula") String cedula, @Param("idPersona") Long idPersona);

    /**
     * Comprueba unicidad de correo excluyendo a la propia persona, para
     * permitir actualizar una ficha sin chocar consigo misma.
     *
     * @param correo correo a comprobar
     * @param idPersona identificador de la persona que se excluye de la comprobación
     * @return {@code true} si otra persona ya usa ese correo
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.correo = :correo AND p.idPersona != :idPersona")
    boolean existsAnotherPersonWithEmail(@Param("correo") String correo, @Param("idPersona") Long idPersona);
}
