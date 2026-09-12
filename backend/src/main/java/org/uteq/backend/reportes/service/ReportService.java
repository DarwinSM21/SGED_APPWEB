package org.uteq.backend.reportes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.payment.entity.Payment;
import org.uteq.backend.academico.payment.repository.PaymentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.attendance.entity.Attendance;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.evaluation.entity.EvaluationDetail;
import org.uteq.backend.deportivo.evaluation.entity.StudentEvaluation;
import org.uteq.backend.deportivo.evaluation.repository.StudentEvaluationRepository;
import org.uteq.backend.deportivo.injury.entity.Injury;
import org.uteq.backend.deportivo.injury.repository.InjuryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Arma las filas de cada reporte reutilizando los repositorios de negocio
 * ya existentes (sin duplicar lógica de consulta); {@link ReportPdfService}
 * solo se encarga del formato del PDF. Los filtros opcionales se construyen
 * con Criteria API en vez de {@code (:x IS NULL OR campo = :x)} en JPQL, que
 * dispara "could not determine data type of parameter" en Postgres.
 */
@Service
@RequiredArgsConstructor
public class ReportService {
    /**
     * Tope de filas por reporte. Sin él, {@code findAll(spec, sort)} con los
     * filtros vacíos se trae la tabla entera y el proceso se queda sin heap.
     * Un PDF de un millón de filas tampoco es un documento legible: si el
     * reporte se corta, lo que hace falta es afinar los filtros, y eso se le
     * dice al usuario en el propio documento.
     */
    private static final int TOPE_FILAS = 5000;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportPdfService pdfService;
    private final StudentRepository estudianteRepository;
    private final PaymentRepository pagoRepository;
    private final AttendanceRepository attendanceRepository;
    private final InjuryRepository injuryRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;

    /**
     * PDF de fichas de estudiantes.
     *
     * @param idCategoria categoría por la que filtrar, o {@code null}
     * @param activo      estado por el que filtrar, o {@code null}
     * @return el PDF como arreglo de bytes
     * @throws ResourceNotFoundException si no hay estudiantes para los
     *                                      filtros
     */
    @Transactional(readOnly = true)
    public byte[] studentProfiles(Long idCategoria, Boolean activo) {
        var encontrados = nonEmpty(estudianteRepository.findForReport(idCategoria, activo));
        var filas = encontrados.stream()
                .map(e -> List.of(
                        e.getStudentCode(),
                        e.getPerson().getName() + " " + e.getPerson().getLastName(),
                        e.getCategory().getNombre(),
                        Boolean.TRUE.equals(e.getActive()) ? "Activo" : "Inactivo",
                        e.getEnrollmentDate().format(FECHA)))
                .toList();
        return pdfService.generate(title("Reporte de Fichas de Estudiantes", filas),
                List.of("Código", "Estudiante", "Categoría", "Estado", "Fecha ingreso"), truncate(filas));
    }

    /**
     * PDF de pagos.
     *
     * @param idEstudiante estudiante por el que filtrar, o {@code null}
     * @param desde        límite inferior de fecha de pago, o {@code null}
     * @param hasta        límite superior de fecha de pago, o {@code null}
     * @return el PDF como arreglo de bytes
     * @throws ResourceNotFoundException si no hay pagos para los filtros
     */
    @Transactional(readOnly = true)
    public byte[] payments(Long idEstudiante, LocalDate desde, LocalDate hasta) {
        Specification<Payment> spec = Specification.<Payment>where(equalTo("student.id", idEstudiante))
                .and(this.<Payment>fromDate("paymentDate", desde))
                .and(this.<Payment>toDate("paymentDate", hasta));
        var filas = nonEmpty(pagoRepository.findAll(spec, PageRequest.of(0, TOPE_FILAS + 1, Sort.by(Sort.Direction.DESC, "paymentDate"))).getContent()).stream()
                .map(this::paymentRow)
                .toList();
        return pdfService.generate(title("Reporte de Pagos", filas),
                List.of("Estudiante", "Tipo", "Período", "Monto", "Fecha de pago", "Registrado por"), truncate(filas));
    }

    /**
     * PDF de asistencias.
     *
     * @param idEstudiante estudiante por el que filtrar, o {@code null}
     * @param idCategoria  categoría por la que filtrar, o {@code null}
     * @param desde        límite inferior de fecha de sesión, o {@code null}
     * @param hasta        límite superior de fecha de sesión, o {@code null}
     * @return el PDF como arreglo de bytes
     * @throws ResourceNotFoundException si no hay asistencias para los
     *                                      filtros
     */
    @Transactional(readOnly = true)
    public byte[] attendances(Long idEstudiante, Long idCategoria, LocalDate desde, LocalDate hasta) {
        Specification<Attendance> spec = Specification.<Attendance>where(equalTo("estudiante.id", idEstudiante))
                .and(this.<Attendance>equalTo("estudiante.category.idCategoria", idCategoria))
                .and(this.<Attendance>fromDate("sesion.fecha", desde))
                .and(this.<Attendance>toDate("sesion.fecha", hasta));
        var filas = nonEmpty(attendanceRepository.findAll(spec, PageRequest.of(0, TOPE_FILAS + 1, Sort.by(Sort.Direction.DESC, "sesion.fecha"))).getContent()).stream()
                .map(this::attendanceRow)
                .toList();
        return pdfService.generate(title("Reporte de Asistencias", filas),
                List.of("Estudiante", "Categoría", "Fecha sesión", "Estado", "Método"), truncate(filas));
    }

    /**
     * PDF de evaluaciones.
     *
     * @param idEstudiante estudiante por el que filtrar, o {@code null}
     * @param idCategoria  categoría del día por la que filtrar, o {@code null}
     * @param desde        límite inferior de fecha de evaluación, o {@code null}
     * @param hasta        límite superior de fecha de evaluación, o {@code null}
     * @return el PDF como arreglo de bytes
     * @throws ResourceNotFoundException si no hay evaluaciones para los
     *                                      filtros
     */
    @Transactional(readOnly = true)
    public byte[] evaluations(Long idEstudiante, Long idCategoria, LocalDate desde, LocalDate hasta) {
        Specification<StudentEvaluation> spec = Specification.<StudentEvaluation>where(equalTo("estudiante.id", idEstudiante))
                .and(this.<StudentEvaluation>equalTo("categoriaDia.idCategoria", idCategoria))
                .and(this.<StudentEvaluation>fromDate("evaluacion.fecha", desde))
                .and(this.<StudentEvaluation>toDate("evaluacion.fecha", hasta));
        var filas = nonEmpty(studentEvaluationRepository.findAll(spec, PageRequest.of(0, TOPE_FILAS + 1, Sort.by(Sort.Direction.DESC, "evaluacion.fecha"))).getContent()).stream()
                .map(this::evaluationRow)
                .toList();
        return pdfService.generate(title("Reporte de Evaluaciones", filas),
                List.of("Estudiante", "Categoría", "Fecha", "Posición", "Promedio"), truncate(filas));
    }

    /**
     * PDF de lesiones.
     *
     * @param idEstudiante estudiante por el que filtrar, o {@code null}
     * @param idCategoria  categoría por la que filtrar, o {@code null}
     * @param desde        límite inferior de fecha de lesión, o {@code null}
     * @param hasta        límite superior de fecha de lesión, o {@code null}
     * @return el PDF como arreglo de bytes
     * @throws ResourceNotFoundException si no hay lesiones para los filtros
     */
    @Transactional(readOnly = true)
    public byte[] injuries(Long idEstudiante, Long idCategoria, LocalDate desde, LocalDate hasta) {
        Specification<Injury> spec = Specification.<Injury>where(equalTo("estudiante.id", idEstudiante))
                .and(this.<Injury>equalTo("estudiante.category.idCategoria", idCategoria))
                .and(this.<Injury>fromDate("fechaLesion", desde))
                .and(this.<Injury>toDate("fechaLesion", hasta));
        var filas = nonEmpty(injuryRepository.findAll(spec, PageRequest.of(0, TOPE_FILAS + 1, Sort.by(Sort.Direction.DESC, "fechaLesion"))).getContent()).stream()
                .map(this::injuryRow)
                .toList();
        return pdfService.generate(title("Reporte de Lesiones", filas),
                List.of("Estudiante", "Descripción", "Fecha lesión", "Retorno estimado", "Estado"), truncate(filas));
    }

    private <T> Specification<T> equalTo(String path, Object valor) {
        if (valor == null) return Specification.<T>where(null);
        return (root, query, cb) -> cb.equal(this.<T, Object>path(root, path), valor);
    }

    private <T> Specification<T> fromDate(String path, LocalDate desde) {
        if (desde == null) return Specification.<T>where(null);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(this.<T, LocalDate>path(root, path), desde);
    }

    private <T> Specification<T> toDate(String path, LocalDate hasta) {
        if (hasta == null) return Specification.<T>where(null);
        return (root, query, cb) -> cb.lessThanOrEqualTo(this.<T, LocalDate>path(root, path), hasta);
    }

    @SuppressWarnings("unchecked")
    private <T, Y> jakarta.persistence.criteria.Path<Y> path(
            jakarta.persistence.criteria.Root<T> root, String puntos) {
        jakarta.persistence.criteria.Path<Object> path = null;
        for (String segmento : puntos.split("\\.")) {
            path = path == null ? root.get(segmento) : path.get(segmento);
        }
        return (jakarta.persistence.criteria.Path<Y>) (jakarta.persistence.criteria.Path<?>) path;
    }

    private <T> List<T> nonEmpty(List<T> resultados) {
        if (resultados.isEmpty()) {
            throw new ResourceNotFoundException("No hay datos para los filtros seleccionados");
        }
        return resultados;
    }

    private List<String> paymentRow(Payment p) {
        String periodo = p.getType() == Payment.PaymentType.MEMBRESIA ? p.getMonth() + "/" + p.getYear() : "-";
        var registrador = p.getRegisteredBy().getPerson();
        return List.of(
                p.getStudent().getPerson().getName() + " " + p.getStudent().getPerson().getLastName(),
                p.getType().name(),
                periodo,
                p.getAmount().toPlainString(),
                p.getPaymentDate().format(FECHA),
                registrador.getName() + " " + registrador.getLastName());
    }

    private List<String> attendanceRow(Attendance a) {
        return List.of(
                a.getEstudiante().getPerson().getName() + " " + a.getEstudiante().getPerson().getLastName(),
                a.getEstudiante().getCategory().getNombre(),
                a.getSesion().getFecha().format(FECHA),
                a.getEstado(),
                a.getMetodo());
    }

    private List<String> evaluationRow(StudentEvaluation ee) {
        String posicion = ee.getPosicionJugada() != null ? ee.getPosicionJugada().getNombre() : "-";
        String average = ee.getDetalles().isEmpty() ? "-" : average(ee.getDetalles());
        return List.of(
                ee.getEstudiante().getPerson().getName() + " " + ee.getEstudiante().getPerson().getLastName(),
                ee.getCategoriaDia().getNombre(),
                ee.getEvaluacion().getFecha().format(FECHA),
                posicion,
                average);
    }

    private List<String> injuryRow(Injury l) {
        String retorno = l.getFechaEstimadaRetorno() != null ? l.getFechaEstimadaRetorno().format(FECHA) : "-";
        String estado = l.isActive() ? "Activa" : "De alta el " + l.getFechaAlta().format(FECHA);
        return List.of(
                l.getEstudiante().getPerson().getName() + " " + l.getEstudiante().getPerson().getLastName(),
                l.getDescripcion(),
                l.getFechaLesion().format(FECHA),
                retorno,
                estado);
    }

    private String average(List<EvaluationDetail> detalles) {
        BigDecimal suma = detalles.stream().map(EvaluationDetail::getPuntaje).reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(detalles.size()), 2, RoundingMode.HALF_UP).toPlainString();
    }
    private boolean exceedsLimit(List<List<String>> filas) {
        return filas.size() > TOPE_FILAS;
    }

    private List<List<String>> truncate(List<List<String>> filas) {
        return exceedsLimit(filas) ? filas.subList(0, TOPE_FILAS) : filas;
    }

    private String title(String base, List<List<String>> filas) {
        return exceedsLimit(filas)
                ? base + " (primeras " + TOPE_FILAS + " filas — afine los filtros para ver el resto)"
                : base;
    }

}
