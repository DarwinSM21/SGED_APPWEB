package org.uteq.backend.academico.guardian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.dto.ConsentDtos.*;
import org.uteq.backend.academico.guardian.entity.Consent;
import org.uteq.backend.academico.guardian.entity.Guardian;
import org.uteq.backend.academico.guardian.repository.ConsentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Otorga y revoca el consentimiento del representante para el tratamiento
 * de datos de un representado (hallazgo H-04 de {@code ETHICS.md}). Lo
 * registra un administrador, dejando constancia de quién lo hizo.
 */
@Service
@RequiredArgsConstructor
public class ConsentService {
    private final ConsentRepository consentimientoRepository;
    private final GuardianRepository representanteRepository;
    private final StudentRepository estudianteRepository;
    private final UserAccountRepository usuarioRepository;

    /**
     * Registra un consentimiento otorgado por un representante sobre un
     * estudiante, con un alcance dado.
     *
     * @param request       representante, estudiante y alcance
     * @param usernameAdmin  administrador que registra el consentimiento
     * @return el consentimiento registrado
     * @throws ResourceNotFoundException si el representante o el estudiante
     *                                      no existen
     * @throws IllegalArgumentException     si ya existe un consentimiento
     *                                      vigente con ese alcance
     */
    @Transactional
    public ConsentResponse grant(GrantConsentRequest request, String usernameAdmin) {
        Guardian representante = representanteRepository.findById(request.idRepresentante())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Representante no encontrado con id: " + request.idRepresentante()));
        Student estudiante = estudianteRepository.findById(request.idEstudiante())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con id: " + request.idEstudiante()));

        consentimientoRepository.findByGuardian_IdAndStudent_IdAndScopeAndRevokedAtIsNull(
                        request.idRepresentante(), request.idEstudiante(), request.alcance())
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Ya existe un consentimiento vigente con ese alcance");
                });

        UserAccount admin = usuarioRepository.findByUsername(usernameAdmin).orElse(null);

        Consent consentimiento = Consent.builder()
                .guardian(representante)
                .student(estudiante)
                .scope(request.alcance())
                .grantedAt(OffsetDateTime.now())
                .registeredBy(admin)
                .build();

        consentimiento = consentimientoRepository.save(consentimiento);
        return toResponse(consentimiento);
    }

    /**
     * Revoca un consentimiento vigente.
     *
     * @param idConsentimiento identificador del consentimiento
     * @param usernameAdmin    administrador que revoca
     * @return el consentimiento revocado
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si ya estaba revocado
     */
    @Transactional
    public ConsentResponse revoke(Long idConsentimiento, String usernameAdmin) {
        Consent consentimiento = consentimientoRepository.findById(idConsentimiento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Consentimiento no encontrado con id: " + idConsentimiento));

        if (!consentimiento.isActive()) {
            throw new IllegalArgumentException("Ese consentimiento ya estaba revocado");
        }

        UserAccount admin = usuarioRepository.findByUsername(usernameAdmin).orElse(null);
        consentimiento.setRevokedAt(OffsetDateTime.now());
        consentimiento.setRevokedBy(admin);
        consentimiento = consentimientoRepository.save(consentimiento);
        return toResponse(consentimiento);
    }

    /**
     * Lista los consentimientos de un estudiante, del más reciente al más
     * antiguo.
     *
     * @param idEstudiante identificador del estudiante
     * @return la lista de consentimientos (vigentes y revocados)
     */
    @Transactional(readOnly = true)
    public List<ConsentResponse> listByStudent(Long idEstudiante) {
        return consentimientoRepository.findByStudent_IdOrderByGrantedAtDesc(idEstudiante).stream()
                .map(this::toResponse)
                .toList();
    }

    private ConsentResponse toResponse(Consent c) {
        return new ConsentResponse(
                c.getId(),
                c.getGuardian().getId(),
                c.getStudent().getId(),
                c.getScope(),
                c.getGrantedAt(),
                c.getRegisteredBy() != null ? c.getRegisteredBy().getUsername() : null,
                c.getRevokedAt(),
                c.isActive());
    }
}
