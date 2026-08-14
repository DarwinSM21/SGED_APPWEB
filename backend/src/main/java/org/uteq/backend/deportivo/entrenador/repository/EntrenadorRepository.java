package org.uteq.backend.deportivo.entrenador.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;

import java.util.Optional;

public interface EntrenadorRepository extends JpaRepository<Entrenador, Long> {

    Page<Entrenador> findByActivoTrue(Pageable pageable);

    boolean existsByPersona_IdPersona(Long idPersona);

    /** Coherencia rol-ficha: solo una ficha vigente condiciona el rol de la cuenta. */
    boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);

    /** Vincula una cuenta nueva a una ficha que ya existia (ver UsuarioService.crear). */
    Optional<Entrenador> findByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByUsuario_IdUsuario(Long idUsuario);

    /** Resuelve el entrenador a partir del usuario autenticado (JWT -> username). */
    Optional<Entrenador> findByUsuario_Username(String username);
}