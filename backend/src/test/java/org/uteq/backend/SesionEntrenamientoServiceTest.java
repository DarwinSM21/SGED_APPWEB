package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionDiariaRepository;
import org.uteq.backend.deportivo.horario.service.HorarioService;
import org.uteq.backend.deportivo.sesion.dto.SesionCrearRequest;
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

/**
 * La propiedad que importa: un ENTRENADOR nunca ve la agenda de otro
 * entrenador, aunque ambos tengan sesiones el mismo dia. El filtro se hace
 * contra el username pasado por el controller (resuelto ahi desde
 * SecurityContextHolder), no contra un parametro que el cliente controle.
 *
 * Prueba unitaria de SesionEntrenamientoService, sin contexto HTTP ni de
 * seguridad (D-03 / R-03 del informe de evaluacion de calidad).
 */
@ExtendWith(MockitoExtension.class)
class SesionEntrenamientoServiceTest {

    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private EntrenadorRepository entrenadorRepository;
    @Mock private EvaluacionDiariaRepository evaluacionRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private HorarioService horarioService;

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
        // Ni siquiera se consulta que sesiones hay: sin entrenador asociado
        // no hay con que compararlas.
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

        verify(horarioService).generarSesionesDeHoy();
    }

    @Test
    @DisplayName("mias devuelve el historial completo del entrenador, no solo las de hoy")
    void miasDevuelveHistorialCompleto() {
        var yo = entrenador(1L, "Carlos");
        var pasada = sesionDe(yo);
        pasada.setFecha(LocalDate.now().minusDays(4));

        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByEntrenadorIdEntrenadorOrderByFechaDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(pasada)));
        when(evaluacionRepository.existsBySesionIdSesion(any())).thenReturn(false);

        List<SesionHoyResponse> resultado = sesionService.misSesiones("carlos@sged.test", 0, 20);

        assertThat(resultado).hasSize(1);
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
}
