package org.uteq.backend.academico.guardian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.guardian.dto.ReportDtos.*;
import org.uteq.backend.academico.guardian.entity.Guardian;
import org.uteq.backend.academico.guardian.repository.GuardianStudentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.common.ia.AIFeedbackGenerator;
import org.uteq.backend.common.ia.AnonymousPlayerProfile;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lectura de informes para el representante o el estudiante autenticado.
 *
 * <p>Recibe {@code username} como parámetro plano en vez de leer el contexto
 * de seguridad aquí: el principal se resuelve siempre en el controlador, así
 * este servicio se prueba con un {@code String} cualquiera sin simular
 * contexto.
 *
 * <p>El chequeo de pertenencia (vínculo activo representante-estudiante) es
 * lo único que autoriza esta lectura, y responde {@code 404} uniforme tanto
 * si el estudiante no existe como si no es un representado suyo: distinguir
 * los casos confirmaría que un id ajeno corresponde a un estudiante real.
 */
@Service
@RequiredArgsConstructor
public class StudentReportService {
    private final GuardianRepository representanteRepository;
    private final GuardianStudentRepository vinculoRepository;
    private final EstudianteRepository estudianteRepository;
    private final LesionRepository lesionRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final AIFeedbackGenerator generadorFeedback;

    /**
     * Lista de estudiantes a cargo del representante dueño de la cuenta.
     *
     * @param username nombre de usuario del representante autenticado
     * @return el resumen de cada representado
     * @throws ResourceNotFoundException si la cuenta no tiene un
     *                                      representante asociado
     */
    @Transactional(readOnly = true)
    public List<StudentSummaryResponse> myStudents(String username) {
        Guardian representante = representanteDe(username);
        return vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(representante.getIdRepresentante())
                .stream()
                .map(v -> {
                    Estudiante e = v.getEstudiante();
                    return new StudentSummaryResponse(
                            e.getIdEstudiante(),
                            e.getPersona().getNombre() + " " + e.getPersona().getApellido(),
                            e.getCategoria().getNombre());
                })
                .toList();
    }

    /**
     * Informe de evaluación de un representado del representante autenticado.
     *
     * @param username     nombre de usuario del representante
     * @param idEstudiante identificador del estudiante
     * @return el informe (promedios por criterio, lesiones, % de asistencia)
     * @throws ResourceNotFoundException si la cuenta no tiene representante
     *                                      asociado, o el estudiante no
     *                                      existe o no es representado suyo
     */
    @Transactional(readOnly = true)
    public StudentReportResponse reportFor(String username, Long idEstudiante) {
        Guardian representante = representanteDe(username);

        boolean esSuyo = vinculoRepository.existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(
                representante.getIdRepresentante(), idEstudiante);
        if (!esSuyo) {
            throw new ResourceNotFoundException("Estudiante no encontrado con id: " + idEstudiante);
        }

        Estudiante estudiante = vinculoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(representante.getIdRepresentante(), idEstudiante)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + idEstudiante))
                .getEstudiante();

        return construirInforme(estudiante);
    }

    /**
     * Informe del propio estudiante autenticado: mismas piezas que
     * {@link #reportFor}, pero sin chequeo de vínculo —la única autorización
     * que hace falta es "es su propia cuenta"—.
     *
     * @param username nombre de usuario del estudiante autenticado
     * @return el informe del estudiante
     * @throws ResourceNotFoundException si la cuenta no tiene un estudiante
     *                                      asociado
     */
    @Transactional(readOnly = true)
    public StudentReportResponse myReport(String username) {
        Estudiante estudiante = estudianteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un estudiante asociado a esta cuenta"));
        return construirInforme(estudiante);
    }

    /**
     * El informe de un representado puesto en palabras por un servicio
     * externo. Se apoya en {@link #reportFor} en lugar de repetir el chequeo
     * de pertenencia: si cambia la regla de quién ve a quién, un segundo
     * chequeo copiado aquí quedaría desactualizado sin que nadie lo note.
     *
     * @param username     nombre de usuario del representante
     * @param idEstudiante identificador del estudiante
     * @return el comentario generado, o un texto por defecto si aún no hay
     *         evaluaciones
     * @throws ResourceNotFoundException si el estudiante no es representado
     *                                      del representante
     */
    @Transactional(readOnly = true)
    public ReportCommentResponse commentFor(String username, Long idEstudiante) {
        StudentReportResponse informe = reportFor(username, idEstudiante);
        return comentarSobre(informe);
    }

    /**
     * Lo mismo que {@link #commentFor}, para el estudiante que consulta su
     * propio informe.
     *
     * @param username nombre de usuario del estudiante autenticado
     * @return el comentario generado, o un texto por defecto si aún no hay
     *         evaluaciones
     * @throws ResourceNotFoundException si la cuenta no tiene un estudiante
     *                                      asociado
     */
    @Transactional(readOnly = true)
    public ReportCommentResponse myComment(String username) {
        return comentarSobre(myReport(username));
    }

    // Arma el perfil seudonimizado y pide el texto. Al modelo va un
    // AnonymousPlayerProfile, que no tiene nombre, cédula, correo ni fecha de
    // nacimiento: solo salen del sistema promedios, categoría y cuántos
    // entrenamientos asistió. El titular de estos datos es un menor.
    private ReportCommentResponse comentarSobre(StudentReportResponse informe) {
        if (informe.promediosPorCriterio().isEmpty()) {
            return new ReportCommentResponse(null, false,
                    "Todavía no hay evaluaciones registradas para comentar");
        }

        Map<String, Double> promedios = new HashMap<>();
        for (CriterionAverageResponse c : informe.promediosPorCriterio()) {
            promedios.put(c.criterio(), c.promedio());
        }

        boolean lesionado = informe.historialLesiones().stream()
                .anyMatch(InjurySummaryResponse::activa);

        LocalDate hoy = LocalDate.now(Zones.ECUADOR);
        long asistencias = asistenciaRepository
                .contarAsistenciasDesde(informe.idEstudiante(), hoy.minusDays(30));

        var perfil = new AnonymousPlayerProfile(
                "Jugador",
                informe.categoria(),
                null,
                promedios,
                Map.of(),
                (int) asistencias,
                lesionado);

        var resultado = generadorFeedback.generatePlayerComment(perfil);
        return new ReportCommentResponse(
                resultado.text(), resultado.isAvailable(), resultado.reason());
    }

    private StudentReportResponse construirInforme(Estudiante estudiante) {
        Long idEstudiante = estudiante.getIdEstudiante();

        List<CriterionAverageResponse> promedios = evaluacionEstudianteRepository
                .promedioHistoricoPorCriterio(idEstudiante).stream()
                .map(fila -> new CriterionAverageResponse(
                        (String) fila[0],
                        fila[1] == null ? 0.0 : ((Number) fila[1]).doubleValue()))
                .toList();

        List<InjurySummaryResponse> lesiones = lesionRepository
                .findByEstudianteIdEstudianteOrderByFechaLesionDesc(idEstudiante, Pageable.unpaged())
                .getContent().stream()
                .map(this::aLesionResumen)
                .toList();

        LocalDate hoy = LocalDate.now(Zones.ECUADOR);
        BigDecimal porcentajeAsistencia = asistenciaRepository
                .calcularPorcentajeAsistencia(idEstudiante, hoy.minusDays(30), hoy);

        return new StudentReportResponse(
                idEstudiante,
                estudiante.getPersona().getNombre() + " " + estudiante.getPersona().getApellido(),
                estudiante.getCategoria().getNombre(),
                promedios,
                lesiones,
                porcentajeAsistencia);
    }

    private Guardian representanteDe(String username) {
        return representanteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un representante asociado a esta cuenta"));
    }

    private InjurySummaryResponse aLesionResumen(Lesion l) {
        return new InjurySummaryResponse(
                l.getIdLesion(), l.getDescripcion(), l.getFechaLesion(),
                l.getFechaEstimadaRetorno(), l.getFechaAlta(), l.estaActiva());
    }
}
