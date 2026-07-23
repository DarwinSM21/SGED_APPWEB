package org.uteq.backend.seguridad.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.seguridad.auth.entity.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
}
