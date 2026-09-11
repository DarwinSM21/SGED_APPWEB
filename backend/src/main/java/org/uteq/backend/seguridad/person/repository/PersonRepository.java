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
    @Query("SELECT p FROM Person p WHERE p.active = true")
    Page<Person> findByActiveTrue(Pageable pageable);

    /**
     * @param cedula cédula a buscar (puede no estar presente, ver RF-49)
     * @return la persona activa con esa cédula, si existe
     */
    @Query("SELECT p FROM Person p WHERE p.nationalId = :cedula AND p.active = true")
    Optional<Person> findByNationalIdAndActiveTrue(@Param("cedula") String cedula);

    /**
     * @param idPersona identificador de la persona
     * @return la persona, si existe y está activa
     */
    @Query("SELECT p FROM Person p WHERE p.id = :idPersona AND p.active = true")
    Optional<Person> findByIdAndActiveTrue(@Param("idPersona") Long idPersona);

    /**
     * @param correo correo electrónico a buscar
     * @return la persona con ese correo, activa o no, si existe
     */
    @Query("SELECT p FROM Person p WHERE p.email = :correo")
    Optional<Person> findByEmail(@Param("correo") String correo);

    /**
     * @param cedula cédula a comprobar
     * @return {@code true} si existe una persona activa con esa cédula
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.nationalId = :cedula AND p.active = true")
    boolean existsByNationalIdAndActiveTrue(@Param("cedula") String cedula);

    /**
     * @param correo correo electrónico a comprobar
     * @return {@code true} si ya existe una persona con ese correo
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.email = :correo")
    boolean existsByEmail(@Param("correo") String correo);

    /**
     * Comprueba unicidad de cédula excluyendo a la propia persona, para
     * permitir actualizar una ficha sin chocar consigo misma.
     *
     * @param cedula cédula a comprobar
     * @param idPersona identificador de la persona que se excluye de la comprobación
     * @return {@code true} si otra persona activa ya usa esa cédula
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.nationalId = :cedula AND p.active = true AND p.id != :idPersona")
    boolean existsAnotherPersonWithNationalId(@Param("cedula") String cedula, @Param("idPersona") Long idPersona);

    /**
     * Comprueba unicidad de correo excluyendo a la propia persona, para
     * permitir actualizar una ficha sin chocar consigo misma.
     *
     * @param correo correo a comprobar
     * @param idPersona identificador de la persona que se excluye de la comprobación
     * @return {@code true} si otra persona ya usa ese correo
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.email = :correo AND p.id != :idPersona")
    boolean existsAnotherPersonWithEmail(@Param("correo") String correo, @Param("idPersona") Long idPersona);
}
