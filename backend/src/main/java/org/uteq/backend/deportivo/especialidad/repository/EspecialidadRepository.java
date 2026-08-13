package org.uteq.backend.deportivo.especialidad.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.especialidad.entity.Especialidad;

import java.util.List;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    Page<Especialidad> findByActivoTrue(Pageable pageable);

    List<Especialidad> findByActivoTrue();

    boolean existsByNombreIgnoreCase(String nombre);
}
