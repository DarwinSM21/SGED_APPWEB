package org.uteq.backend.seguridad.person.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Page<Person> findByActivoTrue(Pageable pageable);

    Optional<Person> findByCedulaAndActivoTrue(String cedula);

    Optional<Person> findByIdPersonaAndActivoTrue(Long idPersona);

    Optional<Person> findByCorreo(String correo);

    boolean existsByCedulaAndActivoTrue(String cedula);

    boolean existsByCorreo(String correo);

    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.cedula = :cedula AND p.activo = true AND p.idPersona != :idPersona")
    boolean existsAnotherPersonWithCedula(@Param("cedula") String cedula, @Param("idPersona") Long idPersona);

    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.correo = :correo AND p.idPersona != :idPersona")
    boolean existsAnotherPersonWithEmail(@Param("correo") String correo, @Param("idPersona") Long idPersona);
}
