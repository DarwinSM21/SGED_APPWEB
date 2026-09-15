package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.guardian.entity.Consent;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a los consentimientos que un representante otorga o revoca sobre un
 * estudiante, por alcance (ej. notificaciones, datos físico-deportivos).
 */
public interface ConsentRepository extends JpaRepository<Consent, Long> {

    /**
     * Historial completo de consentimientos de un estudiante, del más
     * reciente al más antiguo, incluidos los ya revocados.
     *
     * @param idEstudiante identificador del estudiante
     * @return consentimientos ordenados por fecha de otorgamiento descendente
     */
    @Query("SELECT c FROM Consent c WHERE c.student.id = :idEstudiante ORDER BY c.grantedAt DESC")
    List<Consent> findByStudent_IdOrderByGrantedAtDesc(@Param("idEstudiante") Long idEstudiante);

    /**
     * Consentimiento vigente (no revocado) de un representante sobre un
     * estudiante para un alcance puntual.
     *
     * @param idRepresentante identificador del representante
     * @param idEstudiante identificador del estudiante
     * @param alcance alcance del consentimiento (ej. {@code DATOS_FISICO_DEPORTIVOS})
     * @return el consentimiento vigente para ese alcance, si existe
     */
    @Query("SELECT c FROM Consent c WHERE c.guardian.id = :idRepresentante AND c.student.id = :idEstudiante "
            + "AND c.scope = :alcance AND c.revokedAt IS NULL")
    Optional<Consent> findByGuardian_IdAndStudent_IdAndScopeAndRevokedAtIsNull(
            @Param("idRepresentante") Long idRepresentante, @Param("idEstudiante") Long idEstudiante, @Param("alcance") String alcance);
}
