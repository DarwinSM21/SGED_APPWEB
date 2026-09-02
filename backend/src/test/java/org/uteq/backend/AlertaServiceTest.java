package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.academico.alerta.dto.AlertaDtos.EstudianteEnRiesgoResponse;
import org.uteq.backend.academico.alerta.dto.AlertaDtos.PanelAlertasResponse;
import org.uteq.backend.academico.alerta.service.AlertaService;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private AsistenciaRepository asistenciaRepository;

    private AlertaService service;

    @BeforeEach
    void setUp() {
        service = new AlertaService(
                estudianteRepository, pagoRepository, lesionRepository, asistenciaRepository);
        // @Value no se procesa fuera de un contexto Spring: se fijan a mano
        // los mismos valores por defecto declarados en application.yml.
        ReflectionTestUtils.setField(service, "umbralAsistencia", 75);
        ReflectionTestUtils.setField(service, "diasAsistencia", 30);
        ReflectionTestUtils.setField(service, "topeDetalle", 25);
    }

    private Estudiante estudiante(long id, String nombre, String apellido, Categoria categoria) {
        Persona persona = nombre == null ? null
                : Persona.builder().nombre(nombre).apellido(apellido).build();
        return Estudiante.builder()
                .idEstudiante(id)
                .persona(persona)
                .categoria(categoria)
                .build();
    }

    @Test
    @DisplayName("Un estudiante sin ninguna alerta no aparece en el panel")
    void estudianteSinAlertasQuedaFueraDelPanel() {
        Categoria sub12 = Categoria.builder().nombre("SUB-12").build();
        Estudiante e1 = estudiante(1L, "Ana", "Perez", sub12);

        when(estudianteRepository.findByActivoTrueOrderByPersona_ApellidoAsc())
                .thenReturn(List.of(e1));
        when(pagoRepository.idsConMembresiaCubierta(any(TipoPago.class), any(), any()))
                .thenReturn(List.of(1L)); // al dia
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(asistenciaRepository.resumenAsistenciaDeActivos(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 10L, 9L})); // 90% > umbral 75

        PanelAlertasResponse panel = service.panel();

        assertEquals(1, panel.estudiantesActivos());
        assertEquals(0, panel.totalEnRiesgo());
        assertTrue(panel.estudiantes().isEmpty());
    }

    @Test
    @DisplayName("Mensualidad pendiente, asistencia baja y lesion activa se acumulan en un mismo estudiante")
    void lasTresAlertasSeAcumulan() {
        Categoria sub15 = Categoria.builder().nombre("SUB-15").build();
        Estudiante e1 = estudiante(2L, "Luis", "Gomez", sub15);

        when(estudianteRepository.findByActivoTrueOrderByPersona_ApellidoAsc())
                .thenReturn(List.of(e1));
        when(pagoRepository.idsConMembresiaCubierta(any(TipoPago.class), any(), any()))
                .thenReturn(List.of()); // nadie al dia -> debe
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of(2L));
        when(asistenciaRepository.resumenAsistenciaDeActivos(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{2L, 10L, 5L})); // 50% < umbral 75

        PanelAlertasResponse panel = service.panel();

        assertEquals(1, panel.totalEnRiesgo());
        assertEquals(1, panel.conMensualidadPendiente());
        assertEquals(1, panel.conAsistenciaBaja());
        assertEquals(1, panel.conLesionActiva());

        EstudianteEnRiesgoResponse r = panel.estudiantes().get(0);
        assertEquals("Luis Gomez", r.nombreCompleto());
        assertEquals("SUB-15", r.categoria());
        assertTrue(r.mensualidadPendiente());
        assertTrue(r.asistenciaBaja());
        assertTrue(r.lesionActiva());
        assertEquals(3, r.totalAlertas());
    }

    @Test
    @DisplayName("Un estudiante sin persona ni categoria vinculada no rompe el calculo")
    void estudianteSinPersonaNiCategoriaUsaValoresPorDefecto() {
        Estudiante sinPersona = estudiante(3L, null, null, null);

        when(estudianteRepository.findByActivoTrueOrderByPersona_ApellidoAsc())
                .thenReturn(List.of(sinPersona));
        when(pagoRepository.idsConMembresiaCubierta(any(TipoPago.class), any(), any()))
                .thenReturn(List.of()); // debe
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(asistenciaRepository.resumenAsistenciaDeActivos(any(), any()))
                .thenReturn(List.<Object[]>of());

        PanelAlertasResponse panel = service.panel();

        EstudianteEnRiesgoResponse r = panel.estudiantes().get(0);
        assertEquals("(sin persona)", r.nombreCompleto());
        assertNull(r.categoria());
        assertTrue(r.mensualidadPendiente());
        assertFalse(r.asistenciaBaja(), "Sin fila de asistencia, el porcentaje es null y no cuenta como baja");
    }

    @Test
    @DisplayName("Una categoria sin sesiones programadas (programadas=0) se descarta, no divide por cero")
    void categoriaSinSesionesProgramadasSeDescarta() {
        Categoria sub18 = Categoria.builder().nombre("SUB-18").build();
        Estudiante e1 = estudiante(4L, "Rosa", "Diaz", sub18);

        when(estudianteRepository.findByActivoTrueOrderByPersona_ApellidoAsc())
                .thenReturn(List.of(e1));
        when(pagoRepository.idsConMembresiaCubierta(any(TipoPago.class), any(), any()))
                .thenReturn(List.of(4L)); // al dia
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        // programadas = 0: la fila debe ignorarse en vez de intentar dividir entre cero
        when(asistenciaRepository.resumenAsistenciaDeActivos(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{4L, 0L, 0L}));

        PanelAlertasResponse panel = assertDoesNotThrow(() -> service.panel());

        assertEquals(0, panel.totalEnRiesgo(), "Sin dato de asistencia valido, no se marca como en riesgo");
    }

    @Test
    @DisplayName("El detalle se trunca al tope configurado, pero los conteos agregados cuentan a todos")
    void detalleSeTruncaAlTope() {
        ReflectionTestUtils.setField(service, "topeDetalle", 1);
        Categoria sub12 = Categoria.builder().nombre("SUB-12").build();
        Estudiante e1 = estudiante(5L, "Ana", "Ramos", sub12);
        Estudiante e2 = estudiante(6L, "Beto", "Soto", sub12);

        when(estudianteRepository.findByActivoTrueOrderByPersona_ApellidoAsc())
                .thenReturn(List.of(e1, e2));
        when(pagoRepository.idsConMembresiaCubierta(any(TipoPago.class), any(), any()))
                .thenReturn(List.of()); // ninguno al dia -> ambos deben
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(asistenciaRepository.resumenAsistenciaDeActivos(any(), any()))
                .thenReturn(List.<Object[]>of());

        PanelAlertasResponse panel = service.panel();

        assertEquals(2, panel.totalEnRiesgo(), "El conteo agregado no se trunca");
        assertEquals(1, panel.estudiantes().size(), "El detalle si se trunca al tope");
    }
}
