package org.uteq.backend.inventario.assignment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.inventario.assignment.entity.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Page<Assignment> findAllByOrderByFechaAsignacionDesc(Pageable pageable);

    Page<Assignment> findByEstudiante_IdEstudianteOrderByFechaAsignacionDesc(Long idEstudiante, Pageable pageable);

    Page<Assignment> findByEntrenador_IdEntrenadorOrderByFechaAsignacionDesc(Long idEntrenador, Pageable pageable);
}
