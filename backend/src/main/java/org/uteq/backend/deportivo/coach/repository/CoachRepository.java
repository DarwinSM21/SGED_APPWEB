package org.uteq.backend.deportivo.coach.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.coach.entity.Coach;

import java.util.Optional;

public interface CoachRepository extends JpaRepository<Coach, Long> {
    @Query("SELECT e FROM Coach e WHERE e.activo = true")
    Page<Coach> findActiveTrue(Pageable pageable);

    @Query("SELECT COUNT(e) > 0 FROM Coach e WHERE e.persona.id = :idPersona")
    boolean existsByPerson_Id(@Param("idPersona") Long idPersona);

    @Query("SELECT COUNT(e) > 0 FROM Coach e WHERE e.persona.id = :idPersona AND e.activo = true")
    boolean existsByPerson_IdAndActiveTrue(@Param("idPersona") Long idPersona);

    @Query("SELECT e FROM Coach e WHERE e.persona.id = :idPersona AND e.activo = true")
    Optional<Coach> findByPerson_IdAndActiveTrue(@Param("idPersona") Long idPersona);

    @Query("SELECT COUNT(e) > 0 FROM Coach e WHERE e.usuario.id = :idUsuario")
    boolean existsByUserAccount_Id(@Param("idUsuario") Long idUsuario);

    @Query("SELECT e FROM Coach e WHERE e.usuario.username = :username")
    Optional<Coach> findByUserAccount_Username(@Param("username") String username);
}
