package org.uteq.backend.deportivo.specialty.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.specialty.entity.Specialty;

import java.util.List;

/**
 * Acceso al catálogo de especialidades de entrenador (ej. preparación
 * física, portería).
 */
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    /**
     * @param pageable página y tamaño solicitados
     * @return página de especialidades activas
     */
    @Query("SELECT s FROM Specialty s WHERE s.activo = true")
    Page<Specialty> findActiveTrue(Pageable pageable);

    /**
     * @return todas las especialidades activas, sin paginar
     */
    @Query("SELECT s FROM Specialty s WHERE s.activo = true")
    List<Specialty> findActiveTrue();

    /**
     * @param nombre nombre de la especialidad a comprobar
     * @return {@code true} si ya existe una especialidad con ese nombre, sin distinguir mayúsculas/minúsculas
     */
    @Query("SELECT COUNT(s) > 0 FROM Specialty s WHERE LOWER(s.nombre) = LOWER(:nombre)")
    boolean existsByNameIgnoreCase(@Param("nombre") String nombre);
}
