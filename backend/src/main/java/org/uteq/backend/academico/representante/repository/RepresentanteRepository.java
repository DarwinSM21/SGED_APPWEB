package org.uteq.backend.academico.representante.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.representante.entity.Representante;

import java.util.Optional;

public interface RepresentanteRepository extends JpaRepository<Representante, Long> {
    Page<Representante> findByActivoTrue(Pageable pageable);

    boolean existsByPersona_IdPersona(Long idPersona);

    boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);

    Optional<Representante> findByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByUsuario_IdUsuario(Long idUsuario);

    Optional<Representante> findByUsuario_Username(String username);
}
