package org.uteq.backend.academico.representante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.dto.ConsentimientoDtos.*;
import org.uteq.backend.academico.representante.entity.Consentimiento;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.ConsentimientoRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsentimientoService {
    private final ConsentimientoRepository consentimientoRepository;
    private final RepresentanteRepository representanteRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public ConsentimientoResponse otorgar(OtorgarConsentimientoRequest request, String usernameAdmin) {
        Representante representante = representanteRepository.findById(request.idRepresentante())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Representante no encontrado con id: " + request.idRepresentante()));
        Estudiante estudiante = estudianteRepository.findById(request.idEstudiante())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + request.idEstudiante()));

        consentimientoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        request.idRepresentante(), request.idEstudiante(), request.alcance())
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Ya existe un consentimiento vigente con ese alcance");
                });

        Usuario admin = usuarioRepository.findByUsername(usernameAdmin).orElse(null);

        Consentimiento consentimiento = Consentimiento.builder()
                .representante(representante)
                .estudiante(estudiante)
                .alcance(request.alcance())
                .otorgadoEn(OffsetDateTime.now())
                .registradoPor(admin)
                .build();

        consentimiento = consentimientoRepository.save(consentimiento);
        return toResponse(consentimiento);
    }

    @Transactional
    public ConsentimientoResponse revocar(Long idConsentimiento, String usernameAdmin) {
        Consentimiento consentimiento = consentimientoRepository.findById(idConsentimiento)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consentimiento no encontrado con id: " + idConsentimiento));

        if (!consentimiento.estaVigente()) {
            throw new IllegalArgumentException("Ese consentimiento ya estaba revocado");
        }

        Usuario admin = usuarioRepository.findByUsername(usernameAdmin).orElse(null);
        consentimiento.setRevocadoEn(OffsetDateTime.now());
        consentimiento.setRevocadoPor(admin);
        consentimiento = consentimientoRepository.save(consentimiento);
        return toResponse(consentimiento);
    }

    @Transactional(readOnly = true)
    public List<ConsentimientoResponse> listarPorEstudiante(Long idEstudiante) {
        return consentimientoRepository.findByEstudiante_IdEstudianteOrderByOtorgadoEnDesc(idEstudiante).stream()
                .map(this::toResponse)
                .toList();
    }

    private ConsentimientoResponse toResponse(Consentimiento c) {
        return new ConsentimientoResponse(
                c.getIdConsentimiento(),
                c.getRepresentante().getIdRepresentante(),
                c.getEstudiante().getIdEstudiante(),
                c.getAlcance(),
                c.getOtorgadoEn(),
                c.getRegistradoPor() != null ? c.getRegistradoPor().getUsername() : null,
                c.getRevocadoEn(),
                c.estaVigente());
    }
}
