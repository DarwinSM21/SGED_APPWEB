package org.uteq.backend.academico.representante.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.representante.entity.RepresentanteEstudiante;

import java.util.List;
import java.util.Optional;

public interface RepresentanteEstudianteRepository extends JpaRepository<RepresentanteEstudiante, Long> {

    /**
     * El unico metodo que autoriza la lectura de un informe: existe un
     * vinculo, y esta activo. No basta con que exista -pudo haberse
     * desactivado- y no se lee nada mas para decidir esto.
     */
    boolean existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(
            Long idRepresentante, Long idEstudiante);

    List<RepresentanteEstudiante> findByRepresentante_IdRepresentanteAndActivoTrue(Long idRepresentante);

    Optional<RepresentanteEstudiante> findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(
            Long idRepresentante, Long idEstudiante);

    /** "Nombre Apellido - telefono" del representante activo mas antiguo del estudiante, o null si no tiene. */
    @Procedure(procedureName = "academico.sp_contacto_representante_estudiante")
    String contactoDe(@Param("p_estudiante") Long idEstudiante);
}
