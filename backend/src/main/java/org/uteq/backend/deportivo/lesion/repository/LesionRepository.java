package org.uteq.backend.deportivo.lesion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.lesion.entity.Lesion;

import java.util.List;
import java.util.Optional;

public interface LesionRepository extends JpaRepository<Lesion, Long>, JpaSpecificationExecutor<Lesion> {
    @Query("""
           SELECT l FROM Lesion l
           WHERE l.estudiante.idEstudiante = :idEstudiante
             AND l.fechaAlta IS NULL
           """)
    Optional<Lesion> buscarActivaPorEstudiante(@Param("idEstudiante") Long idEstudiante);

    @Query("SELECT l.estudiante.idEstudiante FROM Lesion l WHERE l.fechaAlta IS NULL")
    List<Long> idsEstudiantesLesionados();

    @Query("SELECT l.estudiante.idEstudiante, l.idLesion FROM Lesion l WHERE l.fechaAlta IS NULL")
    List<Object[]> idsYLesionActivaPorEstudiante();

    Page<Lesion> findByEstudianteIdEstudianteOrderByFechaLesionDesc(Long idEstudiante, Pageable pageable);

    @Query("SELECT l FROM Lesion l WHERE l.fechaAlta IS NULL ORDER BY l.fechaLesion DESC")
    Page<Lesion> listarActivas(Pageable pageable);
}
