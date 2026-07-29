package org.uteq.backend.seguridad.persona.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Page<Persona> findByActivoTrue(Pageable pageable);

    Optional<Persona> findByCedulaAndActivoTrue(String cedula);

    Optional<Persona> findByIdPersonaAndActivoTrue(Long idPersona);

    Optional<Persona> findByCorreo(String correo);

    // Verificaciones para CREAR (revisan únicamente registros activos)
    boolean existsByCedulaAndActivoTrue(String cedula);
    
    boolean existsByCorreo(String correo);

    // Verificaciones para EDITAR (excluyen el idPersona actual)
    @Query("SELECT COUNT(p) > 0 FROM Persona p WHERE p.cedula = :cedula AND p.activo = true AND p.idPersona != :idPersona")
    boolean existeOtraPersonaConCedula(@Param("cedula") String cedula, @Param("idPersona") Long idPersona);

    @Query("SELECT COUNT(p) > 0 FROM Persona p WHERE p.correo = :correo AND p.idPersona != :idPersona")
    boolean existeOtraPersonaConCorreo(@Param("correo") String correo, @Param("idPersona") Long idPersona);
}