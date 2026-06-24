package org.uteq.backend.estudiante.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.estudiante.entity.Estudiante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Page<Estudiante> findByActivoTrue(Pageable pageable);
}