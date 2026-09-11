package org.uteq.backend.deportivo.entrenador.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;

import java.util.Optional;

public interface EntrenadorRepository extends JpaRepository<Entrenador, Long> {
    Page<Entrenador> findByActivoTrue(Pageable pageable);

    @Query("SELECT COUNT(e) > 0 FROM Entrenador e WHERE e.persona.id = :idPersona")
    boolean existsByPersona_IdPersona(@Param("idPersona") Long idPersona);

    @Query("SELECT COUNT(e) > 0 FROM Entrenador e WHERE e.persona.id = :idPersona AND e.activo = true")
    boolean existsByPersona_IdPersonaAndActivoTrue(@Param("idPersona") Long idPersona);

    @Query("SELECT e FROM Entrenador e WHERE e.persona.id = :idPersona AND e.activo = true")
    Optional<Entrenador> findByPersona_IdPersonaAndActivoTrue(@Param("idPersona") Long idPersona);

    @Query("SELECT COUNT(e) > 0 FROM Entrenador e WHERE e.usuario.id = :idUsuario")
    boolean existsByUsuario_IdUsuario(@Param("idUsuario") Long idUsuario);

    Optional<Entrenador> findByUsuario_Username(String username);
}
