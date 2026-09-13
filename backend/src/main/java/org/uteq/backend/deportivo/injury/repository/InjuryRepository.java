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

/**
 * Acceso a las lesiones registradas para un estudiante, incluida la
 * comprobación de lesión activa (sin alta) que bloquea la evaluación y la
 * convocatoria a partidos.
 */
public interface InjuryRepository extends JpaRepository<Injury, Long>, JpaSpecificationExecutor<Injury> {
    /**
     * @param idEstudiante identificador del estudiante
     * @return la lesión activa (sin fecha de alta) de ese estudiante, si existe
     */
    @Query("""
           SELECT l FROM Injury l
           WHERE l.estudiante.id = :idEstudiante
             AND l.fechaAlta IS NULL
           """)
    Optional<Injury> findActiveByStudent(@Param("idEstudiante") Long idEstudiante);

    /**
     * @return identificadores de los estudiantes con una lesión activa
     */
    @Query("SELECT l.estudiante.id FROM Injury l WHERE l.fechaAlta IS NULL")
    List<Long> injuredStudentIds();

    /**
     * @return filas {@code [idEstudiante, idLesion]} de las lesiones activas
     */
    @Query("SELECT l.estudiante.id, l.idLesion FROM Injury l WHERE l.fechaAlta IS NULL")
    List<Object[]> activeInjuryIdsByStudent();

    /**
     * @param idEstudiante identificador del estudiante
     * @param pageable página y tamaño solicitados
     * @return el historial de lesiones de ese estudiante, de la más reciente a la más antigua
     */
    @Query("SELECT l FROM Injury l WHERE l.estudiante.id = :idEstudiante ORDER BY l.fechaLesion DESC")
    Page<Injury> findByStudentOrderByInjuryDateDesc(@Param("idEstudiante") Long idEstudiante, Pageable pageable);

    /**
     * @param pageable página y tamaño solicitados
     * @return página de lesiones activas (sin alta), de la más reciente a la más antigua
     */
    @Query("SELECT l FROM Injury l WHERE l.fechaAlta IS NULL ORDER BY l.fechaLesion DESC")
    Page<Injury> findActive(Pageable pageable);
}
