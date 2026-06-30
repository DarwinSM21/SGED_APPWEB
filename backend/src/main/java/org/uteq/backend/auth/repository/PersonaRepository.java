package org.uteq.backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.auth.entity.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
}