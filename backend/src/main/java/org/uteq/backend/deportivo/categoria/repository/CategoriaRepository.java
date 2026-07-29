package org.uteq.backend.deportivo.categoria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Page<Categoria> findByActivoTrue(Pageable pageable);

    List<Categoria> findByActivoTrue();

    boolean existsByNombreIgnoreCase(String nombre);
}