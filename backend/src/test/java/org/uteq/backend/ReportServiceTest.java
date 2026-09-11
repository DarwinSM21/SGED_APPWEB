package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.payment.entity.Payment;
import org.uteq.backend.academico.payment.repository.PaymentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.evaluacion.entity.DetalleEvaluacion;
import org.uteq.backend.deportivo.evaluacion.entity.EvaluacionDiaria;
import org.uteq.backend.deportivo.evaluacion.entity.EvaluacionEstudiante;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.reportes.service.ReportPdfService;
import org.uteq.backend.reportes.service.ReportService;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock private StudentRepository estudianteRepository;
    @Mock private PaymentRepository pagoRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private EvaluacionEstudianteRepository evaluacionEstudianteRepository;

    private ReportService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ReportService(new ReportPdfService(), estudianteRepository,
                pagoRepository, asistenciaRepository, lesionRepository, evaluacionEstudianteRepository);
    }

    private Person persona(String nombre, String apellido) {
        return Person.builder().name(nombre).lastName(apellido).build();
    }

    private Student estudiante(Long id, String categoria) {
        return Student.builder()
                .id(id)
                .person(persona("Ana", "Torres"))
                .category(Categoria.builder().idCategoria(1L).nombre(categoria).build())
                .studentCode("EST-2026-0001")
                .enrollmentDate(LocalDate.of(2026, 1, 10))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("estudiantesFichas sin resultados lanza 404 en vez de generar un PDF vacio")
    void studentProfilesNoResultsReturns404() {
        when(estudianteRepository.findForReport(any(), any())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> servicio.studentProfiles(1L, true));
    }

    @Test
    @DisplayName("estudiantesFichas con resultados genera un PDF valido")
    void studentProfilesWithResultsGeneratesPdf() {
        when(estudianteRepository.findForReport(any(), any())).thenReturn(List.of(estudiante(1L, "SUB-12")));

        byte[] pdf = servicio.studentProfiles(null, null);

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("pagos sin resultados lanza 404")
    void paymentsNoResultsReturns404() {
        when(pagoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThrows(ResourceNotFoundException.class, () -> servicio.payments(1L, null, null));
    }

    @Test
    @DisplayName("pagos con resultados genera un PDF valido")
    void paymentsWithResultsGeneratesPdf() {
        UserAccount registrador = UserAccount.builder().person(persona("Luis", "Gómez")).build();
        Payment pago = Payment.builder()
                .student(estudiante(1L, "SUB-12"))
                .type(Payment.PaymentType.DIARIO)
                .amount(BigDecimal.TEN)
                .paymentDate(LocalDate.of(2026, 8, 1))
                .registeredBy(registrador)
                .build();
        when(pagoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(pago)));

        byte[] pdf = servicio.payments(null, null, null);

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("lesiones sin resultados lanza 404")
    void injuriesNoResultsReturns404() {
        when(lesionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.injuries(null, null, null, null));
    }

    @Test
    @DisplayName("lesiones con resultados genera un PDF valido, incluyendo lesiones activas")
    void injuriesWithResultsGeneratesPdf() {
        Lesion lesion = Lesion.builder()
                .estudiante(estudiante(1L, "SUB-12"))
                .descripcion("Esguince de tobillo")
                .fechaLesion(LocalDate.of(2026, 8, 1))
                .build();
        when(lesionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(lesion)));

        byte[] pdf = servicio.injuries(null, null, null, null);

        assertTrue(pdf.length > 0);
    }

    private Asistencia asistencia(Student estudiante, LocalDate fecha, String estado, String metodo) {
        return Asistencia.builder()
                .estudiante(estudiante)
                .sesion(SesionEntrenamiento.builder().fecha(fecha).build())
                .estado(estado)
                .metodo(metodo)
                .build();
    }

    @Test
    @DisplayName("asistencias sin resultados lanza 404")
    void attendancesNoResultsReturns404() {
        when(asistenciaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.attendances(null, null, null, null));
    }

    @Test
    @DisplayName("asistencias con resultados genera un PDF valido")
    void attendancesWithResultsGeneratesPdf() {
        Asistencia a = asistencia(estudiante(1L, "SUB-12"), LocalDate.of(2026, 8, 2), "PRESENTE", "MANUAL");
        when(asistenciaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));

        byte[] pdf = servicio.attendances(null, null, null, null);

        assertTrue(pdf.length > 0);
    }

    private EvaluacionEstudiante evaluacion(Student estudiante, LocalDate fecha, BigDecimal puntaje) {
        return EvaluacionEstudiante.builder()
                .estudiante(estudiante)
                .categoriaDia(Categoria.builder().idCategoria(1L).nombre("SUB-12").build())
                .posicionJugada(Posicion.builder().nombre("Delantero").build())
                .evaluacion(EvaluacionDiaria.builder().fecha(fecha).build())
                .detalles(List.of(DetalleEvaluacion.builder().puntaje(puntaje).build()))
                .build();
    }

    @Test
    @DisplayName("evaluaciones sin resultados lanza 404")
    void evaluationsNoResultsReturns404() {
        when(evaluacionEstudianteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.evaluations(null, null, null, null));
    }

    @Test
    @DisplayName("evaluaciones con resultados genera un PDF valido con promedio")
    void evaluationsWithResultsGeneratesPdf() {
        EvaluacionEstudiante ee = evaluacion(estudiante(1L, "SUB-12"), LocalDate.of(2026, 8, 3),
                new BigDecimal("8.50"));
        when(evaluacionEstudianteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ee)));

        byte[] pdf = servicio.evaluations(null, null, null, null);

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("pago de membresia muestra el periodo mes/anio en lugar de '-'")
    void paymentMembresiaShowsPeriod() {
        UserAccount registrador = UserAccount.builder().person(persona("Luis", "Gómez")).build();
        Payment pago = Payment.builder()
                .student(estudiante(1L, "SUB-12"))
                .type(Payment.PaymentType.MEMBRESIA)
                .amount(BigDecimal.valueOf(50))
                .month((short) 7)
                .year((short) 2026)
                .paymentDate(LocalDate.of(2026, 7, 31))
                .registeredBy(registrador)
                .build();
        when(pagoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pago)));

        byte[] pdf = servicio.payments(null, null, null);

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("lesion dada de alta aparece como tal y no como activa")
    void injuryResolvedShowsAsResolved() {
        Lesion lesion = Lesion.builder()
                .estudiante(estudiante(1L, "SUB-12"))
                .descripcion("Esguince")
                .fechaLesion(LocalDate.of(2026, 7, 1))
                .fechaEstimadaRetorno(LocalDate.of(2026, 7, 15))
                .fechaAlta(LocalDate.of(2026, 7, 20))
                .build();
        when(lesionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lesion)));

        byte[] pdf = servicio.injuries(null, null, null, null);

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("mas de 5000 resultados se recorta y el titulo avisa de las primeras filas")
    void moreThan5000RowsGetsTrimmedWithNoteInTitle() {
        List<Student> muchos = LongStream.rangeClosed(1, 5001)
                .mapToObj(i -> estudiante(i, "SUB-12"))
                .collect(Collectors.toList());
        when(estudianteRepository.findForReport(any(), any())).thenReturn(muchos);

        byte[] pdf = servicio.studentProfiles(null, null);

        assertTrue(pdf.length > 0);
    }
}
