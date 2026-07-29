package org.uteq.backend.deportivo.entrenador.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;

public interface EntrenadorRepository extends JpaRepository<Entrenador, Long> {

    Page<Entrenador> findByActivoTrue(Pageable pageable);

    boolean existsByPersona_IdPersona(Long idPersona);

    boolean existsByUsuario_IdUsuario(Long idUsuario);
}