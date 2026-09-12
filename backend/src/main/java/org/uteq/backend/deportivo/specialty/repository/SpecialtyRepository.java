package org.uteq.backend.deportivo.specialty.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.specialty.entity.Specialty;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    @Query("SELECT s FROM Specialty s WHERE s.activo = true")
    Page<Specialty> findActiveTrue(Pageable pageable);

    @Query("SELECT s FROM Specialty s WHERE s.activo = true")
    List<Specialty> findActiveTrue();

    @Query("SELECT COUNT(s) > 0 FROM Specialty s WHERE LOWER(s.nombre) = LOWER(:nombre)")
    boolean existsByNameIgnoreCase(@Param("nombre") String nombre);
}
