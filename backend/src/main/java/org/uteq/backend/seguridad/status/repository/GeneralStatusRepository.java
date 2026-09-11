package org.uteq.backend.seguridad.status.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;

import java.util.Optional;

/**
 * Acceso al catálogo de estados generales reutilizado por varias entidades
 * del dominio.
 */
public interface GeneralStatusRepository extends JpaRepository<GeneralStatus, Long> {

    /**
     * @param nombre nombre del estado
     * @return el estado con ese nombre, si existe
     */
    @Query("SELECT g FROM GeneralStatus g WHERE g.name = :nombre")
    Optional<GeneralStatus> findByName(@Param("nombre") String nombre);

    /**
     * @param nombre nombre del estado a comprobar
     * @return {@code true} si ya existe un estado con ese nombre
     */
    @Query("SELECT COUNT(g) > 0 FROM GeneralStatus g WHERE g.name = :nombre")
    boolean existsByName(@Param("nombre") String nombre);

}
