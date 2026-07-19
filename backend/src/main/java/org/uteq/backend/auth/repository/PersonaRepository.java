package org.uteq.backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.auth.entity.Persona;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {
}
