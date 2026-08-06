package org.uteq.backend.academico.representante.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.representante.entity.Consentimiento;

import java.util.List;
import java.util.Optional;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {

    List<Consentimiento> findByEstudiante_IdEstudianteOrderByOtorgadoEnDesc(Long idEstudiante);

    Optional<Consentimiento> findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
            Long idRepresentante, Long idEstudiante, String alcance);
}
