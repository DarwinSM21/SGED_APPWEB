package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.guardian.entity.Consent;

import java.util.List;
import java.util.Optional;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

    List<Consent> findByEstudiante_IdEstudianteOrderByOtorgadoEnDesc(Long idEstudiante);

    Optional<Consent> findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
            Long idRepresentante, Long idEstudiante, String alcance);
}
