package org.uteq.backend.academico.representante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.representante.dto.InformeDtos.*;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lectura de informes para el representante autenticado.
 *
 * <p>Recibe {@code username} como parametro plano en vez de leer
 * {@code SecurityContextHolder} aqui: el principal se resuelve siempre en
 * el controller (igual que {@code AuthController.me()} o
 * {@code SesionEntrenamientoController.hoy()}), asi este servicio se
 * prueba con un String cualquiera sin simular contexto de seguridad.
 *
 * <p>El chequeo de pertenencia (vinculo activo representante-estudiante)
 * es lo unico que autoriza esta lectura -no el consentimiento, ver la
 * nota bajo el hallazgo H-04 en ETHICS.md-, y responde 404 uniforme
 * tanto si el estudiante no existe como si no es un representado suyo:
 * distinguir los casos confirmaria que un id ajeno corresponde a un
 * estudiante real (mismo criterio que el canjeo de QR, que responde 410
 * parejo para "no existio" y "ya se uso").
 */
@Service
@RequiredArgsConstructor
public class InformeService {

    private final RepresentanteRepository representanteRepository;
    private final RepresentanteEstudianteRepository vinculoRepository;
    private final LesionRepository lesionRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    private final AsistenciaRepository asistenciaRepository;

    @Transactional(readOnly = true)
    public List<EstudianteResumenResponse> misRepresentados(String username) {
        Representante representante = representanteDe(username);
        return vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(representante.getIdRepresentante())
                .stream()
                .map(v -> {
                    Estudiante e = v.getEstudiante();
                    return new EstudianteResumenResponse(
                            e.getIdEstudiante(),
                            e.getPersona().getNombre() + " " + e.getPersona().getApellido(),
                            e.getCategoria().getNombre());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public InformeEstudianteResponse informeDe(String username, Long idEstudiante) {
        Representante representante = representanteDe(username);

        boolean esSuyo = vinculoRepository.existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(
                representante.getIdRepresentante(), idEstudiante);
        if (!esSuyo) {
            throw new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante);
        }

        Estudiante estudiante = vinculoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(representante.getIdRepresentante(), idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante))
                .getEstudiante();

        List<PromedioCriterioResponse> promedios = evaluacionEstudianteRepository
                .promedioHistoricoPorCriterio(idEstudiante).stream()
                .map(fila -> new PromedioCriterioResponse(
                        (String) fila[0],
                        fila[1] == null ? 0.0 : ((Number) fila[1]).doubleValue()))
                .toList();

        List<LesionResumenResponse> lesiones = lesionRepository
                .findByEstudianteIdEstudianteOrderByFechaLesionDesc(idEstudiante, Pageable.unpaged())
                .getContent().stream()
                .map(this::aLesionResumen)
                .toList();

        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        BigDecimal porcentajeAsistencia = asistenciaRepository
                .calcularPorcentajeAsistencia(idEstudiante, hoy.minusDays(30), hoy);

        return new InformeEstudianteResponse(
                estudiante.getIdEstudiante(),
                estudiante.getPersona().getNombre() + " " + estudiante.getPersona().getApellido(),
                estudiante.getCategoria().getNombre(),
                promedios,
                lesiones,
                porcentajeAsistencia);
    }

    private Representante representanteDe(String username) {
        return representanteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un representante asociado a esta cuenta"));
    }

    private LesionResumenResponse aLesionResumen(Lesion l) {
        return new LesionResumenResponse(
                l.getIdLesion(), l.getDescripcion(), l.getFechaLesion(),
                l.getFechaEstimadaRetorno(), l.getFechaAlta(), l.estaActiva());
    }
}
