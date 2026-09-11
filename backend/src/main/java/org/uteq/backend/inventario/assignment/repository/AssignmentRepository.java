package org.uteq.backend.inventario.assignment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.inventario.assignment.entity.Assignment;

/**
 * Acceso a las asignaciones de artículos de inventario a estudiantes o
 * entrenadores, y su devolución.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * @param pageable página y tamaño solicitados
     * @return todas las asignaciones, de la más reciente a la más antigua
     */
    @Query("SELECT a FROM Assignment a ORDER BY a.assignmentDate DESC")
    Page<Assignment> findAllByOrderByAssignmentDateDesc(Pageable pageable);

    /**
     * @param idEstudiante identificador del estudiante
     * @param pageable página y tamaño solicitados
     * @return las asignaciones de ese estudiante, de la más reciente a la más antigua
     */
    @Query("SELECT a FROM Assignment a WHERE a.student.id = :idEstudiante ORDER BY a.assignmentDate DESC")
    Page<Assignment> findByStudent_IdOrderByAssignmentDateDesc(@Param("idEstudiante") Long idEstudiante, Pageable pageable);

    /**
     * @param idEntrenador identificador del entrenador
     * @param pageable página y tamaño solicitados
     * @return las asignaciones de ese entrenador, de la más reciente a la más antigua
     */
    @Query("SELECT a FROM Assignment a WHERE a.coach.idEntrenador = :idEntrenador ORDER BY a.assignmentDate DESC")
    Page<Assignment> findByCoach_IdEntrenadorOrderByAssignmentDateDesc(@Param("idEntrenador") Long idEntrenador, Pageable pageable);
}
