package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.evaluacion.entity.EvaluacionDiaria;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionDiariaRepository;
import org.uteq.backend.deportivo.horario.service.HorarioService;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.sesion.dto.SesionCrearRequest;
import org.uteq.backend.deportivo.sesion.dto.SesionHistorialResponse;
import org.uteq.backend.deportivo.sesion.dto.SesionHoyResponse;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.deportivo.sesion.service.SesionEntrenamientoService;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesionEntrenamientoServiceTest {
    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private EntrenadorRepository entrenadorRepository;
    @Mock private EvaluacionDiariaRepository evaluacionRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private HorarioService horarioService;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private EstudianteRepository estudianteRepository;

    @InjectMocks private SesionEntrenamientoService sesionService;

    private Entrenador entrenador(long id, String nombre) {
        return Entrenador.builder()
                .idEntrenador(id)
                .persona(Persona.builder().nombre(nombre).apellido("Apellido").build())
                .build();
    }

    private SesionEntrenamiento sesionDe(Entrenador e) {
        return SesionEntrenamiento.builder()
                .idSesion(e.getIdEntrenador() * 100)
                .entrenador(e)
                .categoria(Categoria.builder().idCategoria(1L).nombre("SUB-12").build())
                .estado("PROGRAMADA")
                .build();
    }

    @Test
    @DisplayName("Un entrenador solo ve sus propias sesiones, no las de otro")
    void entrenadorSoloVeLasPropias() {
        var yo = entrenador(1L, "Carlos");
        var otro = entrenador(2L, "Marta");

        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test"))
                .thenReturn(Optional.of(yo));
        when(sesionRepository.findByFechaOrderByHoraInicioAsc(any()))
                .thenReturn(List.of(sesionDe(yo), sesionDe(otro)));
        when(evaluacionRepository.existsBySesionIdSesion(anyLong())).thenReturn(false);

        List<SesionHoyResponse> resultado = sesionService.sesionesDeHoy("carlos@sged.test", false);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).entrenador()).isEqualTo("Carlos Apellido");
    }

    @Test
    @DisplayName("Un administrador ve las sesiones de todos")
    void administradorVeTodas() {
        var e1 = entrenador(1L, "Carlos");
        var e2 = entrenador(2L, "Marta");

        when(sesionRepository.findByFechaOrderByHoraInicioAsc(any()))
                .thenReturn(List.of(sesionDe(e1), sesionDe(e2)));
        when(evaluacionRepository.existsBySesionIdSesion(anyLong())).thenReturn(false);

        List<SesionHoyResponse> resultado = sesionService.sesionesDeHoy("admin@sged.test", true);

        assertThat(resultado).hasSize(2);
        verify(entrenadorRepository, never()).findByUsuario_Username(any());
    }

    @Test
    @DisplayName("Una cuenta ENTRENADOR sin fila de entrenador asociada no ve nada, no falla")
    void sinEntrenadorAsociadoListaVacia() {
        when(entrenadorRepository.findByUsuario_Username("huerfano@sged.test"))
                .thenReturn(Optional.empty());

        List<SesionHoyResponse> resultado = sesionService.sesionesDeHoy("huerfano@sged.test", false);

        assertThat(resultado).isEmpty();

        verify(sesionRepository, never()).findByFechaOrderByHoraInicioAsc(any());
    }

    @Test
    @DisplayName("El indicador tieneEvaluacion refleja si ya existe la cabecera")
    void indicaSiYaTieneEvaluacion() {
        var yo = entrenador(1L, "Carlos");
        var sesion = sesionDe(yo);

        when(sesionRepository.findByFechaOrderByHoraInicioAsc(any())).thenReturn(List.of(sesion));
        when(evaluacionRepository.existsBySesionIdSesion(sesion.getIdSesion())).thenReturn(true);

        List<SesionHoyResponse> resultado = sesionService.sesionesDeHoy("admin@sged.test", true);

        assertThat(resultado.get(0).tieneEvaluacion()).isTrue();
    }

    @Test
    @DisplayName("hoy genera antes de leer, para que una sesion de horario fijo aparezca sin que nadie la cree a mano")
    void hoyGeneraLasSesionesDelHorarioFijoAntesDeListar() {
        when(sesionRepository.findByFechaOrderByHoraInicioAsc(any())).thenReturn(List.of());

        sesionService.sesionesDeHoy("admin@sged.test", true);

        verify(horarioService).generarSesionesProgramadas();
    }

    @Test
    @DisplayName("mias devuelve el historial completo del entrenador, no solo las de hoy")
    void miasDevuelveHistorialCompleto() {
        var yo = entrenador(1L, "Carlos");
        var pasada = sesionDe(yo);
        pasada.setFecha(LocalDate.now().minusDays(4));

        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.sesionesDelEntrenador(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(pasada)));
        when(evaluacionRepository.existsBySesionIdSesion(any())).thenReturn(false);

        List<SesionHoyResponse> resultado = sesionService.misSesiones("carlos@sged.test", false, 0, 20);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("mias con veTodasLasSesiones=true devuelve el historial de todos, no requiere un Entrenador propio")
    void miasVeTodasNoRequiereEntrenadorPropio() {
        var yo = entrenador(1L, "Carlos");
        var sesion = sesionDe(yo);

        when(sesionRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(sesion)));
        when(evaluacionRepository.existsBySesionIdSesion(any())).thenReturn(false);

        List<SesionHoyResponse> resultado = sesionService.misSesiones("admin@sged.test", true, 0, 20);

        assertThat(resultado).hasSize(1);
        verify(entrenadorRepository, never()).findByUsuario_Username("admin@sged.test");
    }

    @Test
    @DisplayName("crear persiste la sesion a nombre del entrenador dueno del username, no de uno enviado en el request")
    void crearUsaElEntrenadorDelUsername() {
        var yo = entrenador(1L, "Carlos");
        var categoria = Categoria.builder().idCategoria(5L).nombre("SUB-15").build();

        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(evaluacionRepository.existsBySesionIdSesion(any())).thenReturn(false);
        when(sesionRepository.save(any(SesionEntrenamiento.class))).thenAnswer(inv -> {
            SesionEntrenamiento s = inv.getArgument(0);
            s.setIdSesion(99L);
            return s;
        });

        var request = new SesionCrearRequest(5L, LocalDate.of(2026, 8, 10),
                LocalTime.of(16, 0), LocalTime.of(17, 30), "Cancha 1");

        SesionHoyResponse creada = sesionService.crear("carlos@sged.test", request);

        assertThat(creada.categoria()).isEqualTo("SUB-15");
        assertThat(creada.entrenador()).isEqualTo("Carlos Apellido");
    }

    @Test
    @DisplayName("crear rechaza una hora de fin que no es posterior a la de inicio")
    void crearRechazaHoraFinNoPosterior() {
        var yo = entrenador(1L, "Carlos");
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));

        var request = new SesionCrearRequest(5L, LocalDate.of(2026, 8, 10),
                LocalTime.of(17, 0), LocalTime.of(16, 0), null);

        assertThatThrownBy(() -> sesionService.crear("carlos@sged.test", request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(sesionRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear lanza 404 (RecursoNoEncontradoException) si la categoria no existe")
    void crearCategoriaInexistenteLanzaExcepcion() {
        var yo = entrenador(1L, "Carlos");
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        var request = new SesionCrearRequest(999L, LocalDate.of(2026, 8, 10),
                LocalTime.of(16, 0), LocalTime.of(17, 0), null);

        assertThatThrownBy(() -> sesionService.crear("carlos@sged.test", request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("crear rechaza una sesion que se solapa con otra de la misma categoria")
    void crearRechazaSiYaHayUnaSesionSuperpuesta() {
        var yo = entrenador(1L, "Carlos");
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoriaRepository.findById(5L))
                .thenReturn(Optional.of(Categoria.builder().idCategoria(5L).nombre("SUB-15").build()));

        var request = new SesionCrearRequest(5L, LocalDate.of(2026, 8, 10),
                LocalTime.of(16, 0), LocalTime.of(17, 0), null);
        when(sesionRepository.existeSolape(5L, request.fecha(), request.horaInicio(), request.horaFin()))
                .thenReturn(true);

        assertThatThrownBy(() -> sesionService.crear("carlos@sged.test", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya hay una sesión");

        verify(sesionRepository, never()).save(any());
    }

    private Estudiante estudianteDe(long id, String nombre, Posicion posicion) {
        return Estudiante.builder()
                .idEstudiante(id)
                .persona(Persona.builder().nombre(nombre).apellido("Apellido").build())
                .posicion(posicion)
                .build();
    }

    @Test
    @DisplayName("historial lanza 404 si la sesion no existe")
    void historialSesionInexistenteLanza404() {
        when(sesionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sesionService.historial(999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("historial cuenta presentes, tarde, ausentes, justificados y sin registro por separado")
    void historialCuentaCadaEstadoPorSeparado() {
        var yo = entrenador(1L, "Carlos");
        var categoria = Categoria.builder().idCategoria(1L).nombre("SUB-12").build();
        var sesion = SesionEntrenamiento.builder()
                .idSesion(500L).entrenador(yo).categoria(categoria).estado("PROGRAMADA")
                .fecha(LocalDate.of(2026, 8, 10))
                .build();
        when(sesionRepository.findById(500L)).thenReturn(Optional.of(sesion));

        var arquero = Posicion.builder().abreviatura("POR").build();
        Estudiante presente = estudianteDe(1L, "Ana", arquero);
        Estudiante tarde = estudianteDe(2L, "Beto", null);
        Estudiante ausente = estudianteDe(3L, "Cindy", null);
        Estudiante justificado = estudianteDe(4L, "Dario", null);
        Estudiante sinRegistro = estudianteDe(5L, "Eva", null);
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(1L))
                .thenReturn(List.of(presente, tarde, ausente, justificado, sinRegistro));

        when(asistenciaRepository.historialDeSesion(500L)).thenReturn(List.of(
                Asistencia.builder().estudiante(presente).estado(Asistencia.ESTADO_PRESENTE)
                        .horaEntrada(LocalTime.of(16, 1)).metodo("QR").build(),
                Asistencia.builder().estudiante(tarde).estado(Asistencia.ESTADO_TARDE).build(),
                Asistencia.builder().estudiante(ausente).estado(Asistencia.ESTADO_AUSENTE).build(),
                Asistencia.builder().estudiante(justificado).estado(Asistencia.ESTADO_JUSTIFICADO)
                        .observacion("Cita medica").build()
                // 'sinRegistro' no tiene fila de asistencia -> cae en el caso por defecto
        ));
        when(evaluacionRepository.findBySesionIdSesion(500L)).thenReturn(Optional.empty());

        SesionHistorialResponse r = sesionService.historial(500L);

        assertThat(r.resumen().convocados()).isEqualTo(5);
        assertThat(r.resumen().presentes()).isEqualTo(1);
        assertThat(r.resumen().tarde()).isEqualTo(1);
        assertThat(r.resumen().ausentes()).isEqualTo(1);
        assertThat(r.resumen().justificados()).isEqualTo(1);
        assertThat(r.resumen().sinRegistro()).isEqualTo(1);
        assertThat(r.tieneEvaluacion()).isFalse();
        assertThat(r.estadoEvaluacion()).isNull();

        var filaPresente = r.asistencias().stream()
                .filter(f -> f.idEstudiante().equals(1L)).findFirst().orElseThrow();
        assertThat(filaPresente.posicion()).isEqualTo("POR");
        assertThat(filaPresente.metodo()).isEqualTo("QR");

        var filaSinRegistro = r.asistencias().stream()
                .filter(f -> f.idEstudiante().equals(5L)).findFirst().orElseThrow();
        assertThat(filaSinRegistro.estado()).isEqualTo("SIN_REGISTRO");
        assertThat(filaSinRegistro.posicion()).isNull();
    }

    @Test
    @DisplayName("historial tambien incluye a quien registro asistencia pero ya no esta en el plantel activo")
    void historialIncluyeAQuienYaNoEstaEnElPlantelActivo() {
        var yo = entrenador(1L, "Carlos");
        var categoria = Categoria.builder().idCategoria(1L).nombre("SUB-12").build();
        var sesion = SesionEntrenamiento.builder()
                .idSesion(501L).entrenador(yo).categoria(categoria).estado("FINALIZADA")
                .build();
        when(sesionRepository.findById(501L)).thenReturn(Optional.of(sesion));

        // Plantel activo vacio: el estudiante se dio de baja despues de la sesion.
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(1L))
                .thenReturn(List.of());

        Estudiante deBaja = estudianteDe(9L, "Fabio", null);
        when(asistenciaRepository.historialDeSesion(501L)).thenReturn(List.of(
                Asistencia.builder().estudiante(deBaja).estado(Asistencia.ESTADO_PRESENTE).build()));
        when(evaluacionRepository.findBySesionIdSesion(501L))
                .thenReturn(Optional.of(EvaluacionDiaria.builder().estado("CERRADA").build()));

        SesionHistorialResponse r = sesionService.historial(501L);

        assertThat(r.resumen().convocados()).isEqualTo(1);
        assertThat(r.resumen().presentes()).isEqualTo(1);
        assertThat(r.asistencias()).hasSize(1);
        assertThat(r.asistencias().get(0).idEstudiante()).isEqualTo(9L);
        assertThat(r.tieneEvaluacion()).isTrue();
        assertThat(r.estadoEvaluacion()).isEqualTo("CERRADA");
    }
}
