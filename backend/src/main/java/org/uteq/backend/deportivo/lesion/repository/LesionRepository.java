package org.uteq.backend.deportivo.lesion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.lesion.entity.Lesion;

import java.util.List;
import java.util.Optional;

public interface LesionRepository extends JpaRepository<Lesion, Long> {

    /** Lesion activa (sin alta) de un estudiante. La BD garantiza que hay a lo sumo una. */
    @Query("""
           SELECT l FROM Lesion l
           WHERE l.estudiante.idEstudiante = :idEstudiante
             AND l.fechaAlta IS NULL
           """)
    Optional<Lesion> buscarActivaPorEstudiante(@Param("idEstudiante") Long idEstudiante);

    /** Identificadores de estudiantes con lesion activa: se excluyen de las plantillas. */
    @Query("SELECT l.estudiante.idEstudiante FROM Lesion l WHERE l.fechaAlta IS NULL")
    List<Long> idsEstudiantesLesionados();

    /**
     * Igual que idsEstudiantesLesionados pero trae tambien el id de la
     * lesion: la pantalla de evaluacion diaria necesita ese id para poder
     * dar de alta sin una consulta aparte por jugador. Cada fila es
     * [idEstudiante, idLesion].
     */
    @Query("SELECT l.estudiante.idEstudiante, l.idLesion FROM Lesion l WHERE l.fechaAlta IS NULL")
    List<Object[]> idsYLesionActivaPorEstudiante();

    Page<Lesion> findByEstudianteIdEstudianteOrderByFechaLesionDesc(Long idEstudiante, Pageable pageable);

    @Query("SELECT l FROM Lesion l WHERE l.fechaAlta IS NULL ORDER BY l.fechaLesion DESC")
    Page<Lesion> listarActivas(Pageable pageable);
}
