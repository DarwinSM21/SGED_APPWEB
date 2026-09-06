package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.guardian.entity.Guardian;

import java.util.Optional;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Page<Guardian> findByActivoTrue(Pageable pageable);

    boolean existsByPersona_IdPersona(Long idPersona);

    boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);

    Optional<Guardian> findByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByUsuario_IdUsuario(Long idUsuario);

    Optional<Guardian> findByUsuario_Username(String username);
}
