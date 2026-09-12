package org.uteq.backend.deportivo.injury.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.injury.entity.Injury;

import java.util.List;
import java.util.Optional;

public interface InjuryRepository extends JpaRepository<Injury, Long>, JpaSpecificationExecutor<Injury> {
    @Query("""
           SELECT l FROM Injury l
           WHERE l.estudiante.id = :idEstudiante
             AND l.fechaAlta IS NULL
           """)
    Optional<Injury> findActiveByStudent(@Param("idEstudiante") Long idEstudiante);

    @Query("SELECT l.estudiante.id FROM Injury l WHERE l.fechaAlta IS NULL")
    List<Long> injuredStudentIds();

    @Query("SELECT l.estudiante.id, l.idLesion FROM Injury l WHERE l.fechaAlta IS NULL")
    List<Object[]> activeInjuryIdsByStudent();

    @Query("SELECT l FROM Injury l WHERE l.estudiante.id = :idEstudiante ORDER BY l.fechaLesion DESC")
    Page<Injury> findByStudentOrderByInjuryDateDesc(@Param("idEstudiante") Long idEstudiante, Pageable pageable);

    @Query("SELECT l FROM Injury l WHERE l.fechaAlta IS NULL ORDER BY l.fechaLesion DESC")
    Page<Injury> findActive(Pageable pageable);
}
