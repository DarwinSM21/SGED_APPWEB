package org.uteq.backend.academico.guardian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
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
    private final StudentRepository estudianteRepository;
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
        Guardian representante = guardianOf(username);
        return vinculoRepository.findByGuardian_IdAndActiveTrue(representante.getId())
                .stream()
                .map(v -> {
                    Student e = v.getStudent();
                    return new StudentSummaryResponse(
                            e.getId(),
                            e.getPerson().getName() + " " + e.getPerson().getLastName(),
                            e.getCategory().getNombre());
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
        Guardian representante = guardianOf(username);

        boolean esSuyo = vinculoRepository.existsByGuardian_IdAndStudent_IdAndActiveTrue(
                representante.getId(), idEstudiante);
        if (!esSuyo) {
            throw new ResourceNotFoundException("Estudiante no encontrado con id: " + idEstudiante);
        }

        Student estudiante = vinculoRepository
                .findByGuardian_IdAndStudent_Id(representante.getId(), idEstudiante)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + idEstudiante))
                .getStudent();

        return buildReport(estudiante);
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
        Student estudiante = estudianteRepository.findByUserAccount_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un estudiante asociado a esta cuenta"));
        return buildReport(estudiante);
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
        return commentOn(informe);
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
        return commentOn(myReport(username));
    }

    // Arma el perfil seudonimizado y pide el texto. Al modelo va un
    // AnonymousPlayerProfile, que no tiene nombre, cédula, correo ni fecha de
    // nacimiento: solo salen del sistema promedios, categoría y cuántos
    // entrenamientos asistió. El titular de estos datos es un menor.
    private ReportCommentResponse commentOn(StudentReportResponse informe) {
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

    private StudentReportResponse buildReport(Student estudiante) {
        Long idEstudiante = estudiante.getId();

        List<CriterionAverageResponse> promedios = evaluacionEstudianteRepository
                .promedioHistoricoPorCriterio(idEstudiante).stream()
                .map(fila -> new CriterionAverageResponse(
                        (String) fila[0],
                        fila[1] == null ? 0.0 : ((Number) fila[1]).doubleValue()))
                .toList();

        List<InjurySummaryResponse> lesiones = lesionRepository
                .findByEstudianteIdEstudianteOrderByFechaLesionDesc(idEstudiante, Pageable.unpaged())
                .getContent().stream()
                .map(this::toInjurySummary)
                .toList();

        LocalDate hoy = LocalDate.now(Zones.ECUADOR);
        BigDecimal porcentajeAsistencia = asistenciaRepository
                .calcularPorcentajeAsistencia(idEstudiante, hoy.minusDays(30), hoy);

        return new StudentReportResponse(
                idEstudiante,
                estudiante.getPerson().getName() + " " + estudiante.getPerson().getLastName(),
                estudiante.getCategory().getNombre(),
                promedios,
                lesiones,
                porcentajeAsistencia);
    }

    private Guardian guardianOf(String username) {
        return representanteRepository.findByUserAccount_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un representante asociado a esta cuenta"));
    }

    private InjurySummaryResponse toInjurySummary(Lesion l) {
        return new InjurySummaryResponse(
                l.getIdLesion(), l.getDescripcion(), l.getFechaLesion(),
                l.getFechaEstimadaRetorno(), l.getFechaAlta(), l.estaActiva());
    }
}
