package org.uteq.backend.deportivo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.entity.Entrenador;

import java.util.List;
import java.util.Optional;

public interface EntrenadorRepository extends JpaRepository<Entrenador, Long> {

    Optional<Entrenador> findByPersonaIdPersona(Long idPersona);

    Page<Entrenador> findByActivoTrue(Pageable pageable);

    List<Entrenador> findByActivoTrue();

    boolean existsByPersonaIdPersona(Long idPersona);
}
