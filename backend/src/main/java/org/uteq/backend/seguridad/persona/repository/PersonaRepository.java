package org.uteq.backend.seguridad.persona.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Page<Persona> findByActivoTrue(Pageable pageable);

    Optional<Persona> findByCedulaAndActivoTrue(String cedula);

    Optional<Persona> findByIdPersonaAndActivoTrue(Long idPersona);

    Optional<Persona> findByCorreo(String correo);

    boolean existsByCedula(String cedula);

    boolean existsByCorreo(String correo);
} 