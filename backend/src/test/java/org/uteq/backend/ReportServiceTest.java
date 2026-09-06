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
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.reportes.service.ReportPdfService;
import org.uteq.backend.reportes.service.ReportService;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        return Person.builder().nombre(nombre).apellido(apellido).build();
    }

    private Student estudiante(Long id, String categoria) {
        return Student.builder()
                .idEstudiante(id)
                .persona(persona("Ana", "Torres"))
                .categoria(Categoria.builder().idCategoria(1L).nombre(categoria).build())
                .codigoEstudiante("EST-2026-0001")
                .fechaIngreso(LocalDate.of(2026, 1, 10))
                .activo(true)
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
        UserAccount registrador = UserAccount.builder().persona(persona("Luis", "Gómez")).build();
        Payment pago = Payment.builder()
                .estudiante(estudiante(1L, "SUB-12"))
                .tipo(Payment.TipoPago.DIARIO)
                .monto(BigDecimal.TEN)
                .fechaPago(LocalDate.of(2026, 8, 1))
                .registradoPor(registrador)
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
}
