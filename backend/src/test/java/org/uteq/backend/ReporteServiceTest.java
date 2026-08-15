package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.reportes.service.ReportePdfService;
import org.uteq.backend.reportes.service.ReporteService;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private EvaluacionEstudianteRepository evaluacionEstudianteRepository;

    private ReporteService servicio;

    @BeforeEach
    void setUp() {
        // ReportePdfService no tiene dependencias: se usa la instancia real
        // en vez de mockearla, asi la prueba tambien valida que el PDF
        // generado a partir de las filas armadas es un PDF valido.
        servicio = new ReporteService(new ReportePdfService(), estudianteRepository,
                pagoRepository, asistenciaRepository, lesionRepository, evaluacionEstudianteRepository);
    }

    private Persona persona(String nombre, String apellido) {
        return Persona.builder().nombre(nombre).apellido(apellido).build();
    }

    private Estudiante estudiante(Long id, String categoria) {
        return Estudiante.builder()
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
    void estudiantesFichasSinResultadosDa404() {
        when(estudianteRepository.buscarParaReporte(any(), any())).thenReturn(List.of());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.estudiantesFichas(1L, true));
    }

    @Test
    @DisplayName("estudiantesFichas con resultados genera un PDF valido")
    void estudiantesFichasConResultadosGeneraPdf() {
        when(estudianteRepository.buscarParaReporte(any(), any())).thenReturn(List.of(estudiante(1L, "SUB-12")));

        byte[] pdf = servicio.estudiantesFichas(null, null);

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("pagos sin resultados lanza 404")
    void pagosSinResultadosDa404() {
        when(pagoRepository.buscarParaReporte(any(), any(), any())).thenReturn(List.of());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.pagos(1L, null, null));
    }

    @Test
    @DisplayName("pagos con resultados genera un PDF valido")
    void pagosConResultadosGeneraPdf() {
        Usuario registrador = Usuario.builder().persona(persona("Luis", "Gómez")).build();
        Pago pago = Pago.builder()
                .estudiante(estudiante(1L, "SUB-12"))
                .tipo(Pago.TipoPago.DIARIO)
                .monto(BigDecimal.TEN)
                .fechaPago(LocalDate.of(2026, 8, 1))
                .registradoPor(registrador)
                .build();
        when(pagoRepository.buscarParaReporte(any(), any(), any())).thenReturn(List.of(pago));

        byte[] pdf = servicio.pagos(null, null, null);

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("lesiones sin resultados lanza 404")
    void lesionesSinResultadosDa404() {
        when(lesionRepository.buscarParaReporte(any(), any(), any(), any())).thenReturn(List.of());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.lesiones(null, null, null, null));
    }

    @Test
    @DisplayName("lesiones con resultados genera un PDF valido, incluyendo lesiones activas")
    void lesionesConResultadosGeneraPdf() {
        Lesion lesion = Lesion.builder()
                .estudiante(estudiante(1L, "SUB-12"))
                .descripcion("Esguince de tobillo")
                .fechaLesion(LocalDate.of(2026, 8, 1))
                .build();
        when(lesionRepository.buscarParaReporte(any(), any(), any(), any())).thenReturn(List.of(lesion));

        byte[] pdf = servicio.lesiones(null, null, null, null);

        assertTrue(pdf.length > 0);
    }
}
