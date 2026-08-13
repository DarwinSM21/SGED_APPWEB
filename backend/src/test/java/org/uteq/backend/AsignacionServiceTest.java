package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.inventario.articulo.entity.Articulo.TipoArticulo;
import org.uteq.backend.inventario.articulo.repository.ArticuloRepository;
import org.uteq.backend.inventario.asignacion.dto.AsignacionDtos.AsignacionRequest;
import org.uteq.backend.inventario.asignacion.dto.AsignacionDtos.AsignacionResponse;
import org.uteq.backend.inventario.asignacion.dto.AsignacionDtos.DevolucionRequest;
import org.uteq.backend.inventario.asignacion.entity.Asignacion;
import org.uteq.backend.inventario.asignacion.entity.Asignacion.EstadoAsignacion;
import org.uteq.backend.inventario.asignacion.entity.Asignacion.TipoDestinatario;
import org.uteq.backend.inventario.asignacion.repository.AsignacionRepository;
import org.uteq.backend.inventario.asignacion.service.AsignacionService;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsignacionServiceTest {

    @Mock private AsignacionRepository asignacionRepository;
    @Mock private ArticuloRepository articuloRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private EntrenadorRepository entrenadorRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AsignacionService asignacionService;

    private Articulo uniformeConStock(int stockActual) {
        return Articulo.builder()
                .idArticulo(1L)
                .nombre("Uniforme Sub-12")
                .tipo(TipoArticulo.UNIFORME)
                .stockActual(stockActual)
                .stockMinimo(2)
                .activo(true)
                .build();
    }

    private Estudiante estudiante() {
        Persona persona = Persona.builder().nombre("Juan").apellido("Perez").build();
        return Estudiante.builder().idEstudiante(5L).persona(persona).build();
    }

    private Entrenador entrenador() {
        Persona persona = Persona.builder().nombre("Carlos").apellido("Ruiz").build();
        return Entrenador.builder().idEntrenador(7L).persona(persona).build();
    }

    private Usuario registrador() {
        Persona persona = Persona.builder().nombre("Ana").apellido("Diaz").build();
        return Usuario.builder().idUsuario(9L).username("recepcion").persona(persona).build();
    }

    private void stubGuardarAsignacion() {
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(inv -> {
            Asignacion a = inv.getArgument(0);
            a.setIdAsignacion(100L);
            return a;
        });
    }

    @Test
    @DisplayName("crear una asignacion a estudiante resta del stock_actual del articulo")
    void crear_asignacion_a_estudiante_resta_stock() {
        Articulo articulo = uniformeConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante()));
        stubGuardarAsignacion();

        AsignacionRequest request = new AsignacionRequest(1L, 1, TipoDestinatario.ESTUDIANTE, 5L, null, null, null);
        AsignacionResponse resultado = asignacionService.crear(request, "recepcion");

        assertThat(articulo.getStockActual()).isEqualTo(9);
        assertThat(resultado.estado()).isEqualTo(EstadoAsignacion.ASIGNADO);
        assertThat(resultado.estudiante()).isEqualTo("Juan Perez");
    }

    @Test
    @DisplayName("crear rechaza cuando la cantidad pedida supera el stock disponible")
    void crear_con_stock_insuficiente_lanza_excepcion() {
        Articulo articulo = uniformeConStock(1);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));

        AsignacionRequest request = new AsignacionRequest(1L, 3, TipoDestinatario.ESTUDIANTE, 5L, null, null, null);

        assertThatThrownBy(() -> asignacionService.crear(request, "recepcion"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(articulo.getStockActual()).isEqualTo(1);
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza ESTUDIANTE sin idEstudiante, sin tocar el stock")
    void crear_estudiante_sin_id_lanza_excepcion() {
        AsignacionRequest request = new AsignacionRequest(1L, 1, TipoDestinatario.ESTUDIANTE, null, null, null, null);

        assertThatThrownBy(() -> asignacionService.crear(request, "recepcion"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(articuloRepository);
    }

    @Test
    @DisplayName("crear una asignacion a entrenador resuelve el entrenador, no el estudiante")
    void crear_asignacion_a_entrenador() {
        Articulo articulo = uniformeConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(entrenadorRepository.findById(7L)).thenReturn(Optional.of(entrenador()));
        stubGuardarAsignacion();

        AsignacionRequest request = new AsignacionRequest(1L, 2, TipoDestinatario.ENTRENADOR, null, 7L, null, null);
        AsignacionResponse resultado = asignacionService.crear(request, "recepcion");

        assertThat(resultado.entrenador()).isEqualTo("Carlos Ruiz");
        assertThat(resultado.estudiante()).isNull();
        verifyNoInteractions(estudianteRepository);
    }

    private Asignacion asignacionActiva() {
        return Asignacion.builder()
                .idAsignacion(100L)
                .articulo(uniformeConStock(9))
                .cantidad(1)
                .tipoDestinatario(TipoDestinatario.ESTUDIANTE)
                .estudiante(estudiante())
                .estado(EstadoAsignacion.ASIGNADO)
                .registradoPor(registrador())
                .build();
    }

    @Test
    @DisplayName("devolver con estado DEVUELTO repone el stock_actual del articulo")
    void devolver_con_devuelto_repone_stock() {
        Asignacion asignacion = asignacionActiva();
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(asignacion));
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(inv -> inv.getArgument(0));

        DevolucionRequest request = new DevolucionRequest(EstadoAsignacion.DEVUELTO, "en buen estado");
        AsignacionResponse resultado = asignacionService.devolver(100L, request);

        assertThat(resultado.estado()).isEqualTo(EstadoAsignacion.DEVUELTO);
        assertThat(asignacion.getArticulo().getStockActual()).isEqualTo(10);
        assertThat(resultado.fechaDevolucionReal()).isNotNull();
    }

    @Test
    @DisplayName("devolver con estado PERDIDO no repone el stock_actual del articulo")
    void devolver_con_perdido_no_repone_stock() {
        Asignacion asignacion = asignacionActiva();
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(asignacion));
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(inv -> inv.getArgument(0));

        DevolucionRequest request = new DevolucionRequest(EstadoAsignacion.PERDIDO, "no se recupero");
        AsignacionResponse resultado = asignacionService.devolver(100L, request);

        assertThat(resultado.estado()).isEqualTo(EstadoAsignacion.PERDIDO);
        assertThat(asignacion.getArticulo().getStockActual()).isEqualTo(9);
    }

    @Test
    @DisplayName("devolver una asignacion ya resuelta lanza excepcion")
    void devolver_asignacion_ya_resuelta_lanza_excepcion() {
        Asignacion asignacion = asignacionActiva();
        asignacion.setEstado(EstadoAsignacion.DEVUELTO);
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(asignacion));

        DevolucionRequest request = new DevolucionRequest(EstadoAsignacion.PERDIDO, null);

        assertThatThrownBy(() -> asignacionService.devolver(100L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya fue resuelta");

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("devolver con estado ASIGNADO es invalido: no es una transicion de devolucion")
    void devolver_con_estado_asignado_lanza_excepcion() {
        DevolucionRequest request = new DevolucionRequest(EstadoAsignacion.ASIGNADO, null);

        assertThatThrownBy(() -> asignacionService.devolver(100L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(asignacionRepository);
    }

    @Test
    @DisplayName("devolver una asignacion inexistente lanza RecursoNoEncontradoException")
    void devolver_asignacion_inexistente_lanza_excepcion() {
        when(asignacionRepository.findById(404L)).thenReturn(Optional.empty());

        DevolucionRequest request = new DevolucionRequest(EstadoAsignacion.DEVUELTO, null);

        assertThatThrownBy(() -> asignacionService.devolver(404L, request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
