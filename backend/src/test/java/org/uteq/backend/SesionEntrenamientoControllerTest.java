package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionDiariaRepository;
import org.uteq.backend.deportivo.horario.service.HorarioService;
import org.uteq.backend.deportivo.sesion.controller.SesionEntrenamientoController;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * La propiedad que importa: un ENTRENADOR nunca ve la agenda de otro
 * entrenador, aunque ambos tengan sesiones el mismo dia. El filtro se hace
 * contra el usuario autenticado, no contra un parametro de la peticion.
 */
@ExtendWith(MockitoExtension.class)
class SesionEntrenamientoControllerTest {

    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private EntrenadorRepository entrenadorRepository;
    @Mock private EvaluacionDiariaRepository evaluacionRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private HorarioService horarioService;

    @InjectMocks private SesionEntrenamientoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        // SecurityContextHolder usa un ThreadLocal: sin esto, una prueba
        // podria heredar la autenticacion que dejo la anterior.
        SecurityContextHolder.clearContext();
    }

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

    private void autenticarComo(String username, String rol) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Un entrenador solo ve sus propias sesiones, no las de otro")
    void entrenadorSoloVeLasPropias() throws Exception {
        var yo = entrenador(1L, "Carlos");
        var otro = entrenador(2L, "Marta");

        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test"))
                .thenReturn(Optional.of(yo));
        when(sesionRepository.findByFechaOrderByHoraInicioAsc(LocalDate.now()))
                .thenReturn(List.of(sesionDe(yo), sesionDe(otro)));
        when(evaluacionRepository.existsBySesionIdSesion(anyLong())).thenReturn(false);

        mockMvc.perform(get("/api/sesiones/hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].entrenador").value("Carlos Apellido"));
    }

    @Test
    @DisplayName("Un administrador ve las sesiones de todos")
    void administradorVeTodas() throws Exception {
        var e1 = entrenador(1L, "Carlos");
        var e2 = entrenador(2L, "Marta");

        autenticarComo("admin@sged.test", "ADMINISTRADOR");
        when(sesionRepository.findByFechaOrderByHoraInicioAsc(LocalDate.now()))
                .thenReturn(List.of(sesionDe(e1), sesionDe(e2)));
        when(evaluacionRepository.existsBySesionIdSesion(anyLong())).thenReturn(false);

        mockMvc.perform(get("/api/sesiones/hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Un recepcionista ve las sesiones de todos, igual que un administrador")
    void recepcionistaVeTodasLasSesiones() throws Exception {
        // Regresion: si se agrega RECEPCIONISTA solo al @PreAuthorize y no a
        // esta rama interna, la cuenta autentica bien pero cae al "else",
        // busca una fila Entrenador que un recepcionista nunca tiene, y ve
        // una lista vacia en silencio en vez de todas las sesiones.
        var e1 = entrenador(1L, "Carlos");
        var e2 = entrenador(2L, "Marta");

        autenticarComo("recepcion@sged.test", "RECEPCIONISTA");
        when(sesionRepository.findByFechaOrderByHoraInicioAsc(LocalDate.now()))
                .thenReturn(List.of(sesionDe(e1), sesionDe(e2)));
        when(evaluacionRepository.existsBySesionIdSesion(anyLong())).thenReturn(false);

        mockMvc.perform(get("/api/sesiones/hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(entrenadorRepository, never()).findByUsuario_Username(any());
    }

    @Test
    @DisplayName("Una cuenta ENTRENADOR sin fila de entrenador asociada no ve nada, no falla")
    void sinEntrenadorAsociadoListaVacia() throws Exception {
        autenticarComo("huerfano@sged.test", "ENTRENADOR");
        when(entrenadorRepository.findByUsuario_Username("huerfano@sged.test"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sesiones/hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Ni siquiera se consulta que sesiones hay: sin entrenador asociado
        // no hay con que compararlas.
        verify(sesionRepository, never()).findByFechaOrderByHoraInicioAsc(any());
    }

    @Test
    @DisplayName("El indicador tieneEvaluacion refleja si ya existe la cabecera")
    void indicaSiYaTieneEvaluacion() throws Exception {
        var yo = entrenador(1L, "Carlos");
        var sesion = sesionDe(yo);

        autenticarComo("admin@sged.test", "ADMINISTRADOR");
        when(sesionRepository.findByFechaOrderByHoraInicioAsc(LocalDate.now()))
                .thenReturn(List.of(sesion));
        when(evaluacionRepository.existsBySesionIdSesion(sesion.getIdSesion())).thenReturn(true);

        mockMvc.perform(get("/api/sesiones/hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tieneEvaluacion").value(true));
    }

    // --- POST /api/sesiones ---

    @Test
    @DisplayName("crear persiste la sesion a nombre del entrenador autenticado, no de uno enviado en el body")
    void crear_usa_el_entrenador_autenticado() throws Exception {
        var yo = entrenador(1L, "Carlos");
        var categoria = Categoria.builder().idCategoria(5L).nombre("SUB-15").build();

        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(evaluacionRepository.existsBySesionIdSesion(any())).thenReturn(false);
        when(sesionRepository.save(any(SesionEntrenamiento.class))).thenAnswer(inv -> {
            SesionEntrenamiento s = inv.getArgument(0);
            s.setIdSesion(99L);
            return s;
        });

        mockMvc.perform(post("/api/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":5,\"fecha\":\"2026-08-10\",\"horaInicio\":\"16:00:00\",\"horaFin\":\"17:30:00\",\"campo\":\"Cancha 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("SUB-15"))
                .andExpect(jsonPath("$.entrenador").value("Carlos Apellido"));
    }

    @Test
    @DisplayName("crear rechaza una hora de fin que no es posterior a la de inicio")
    void crear_rechaza_horaFin_no_posterior() throws Exception {
        var yo = entrenador(1L, "Carlos");
        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));

        mockMvc.perform(post("/api/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":5,\"fecha\":\"2026-08-10\",\"horaInicio\":\"17:00:00\",\"horaFin\":\"16:00:00\"}"))
                .andExpect(status().isBadRequest());

        verify(sesionRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear responde 404 si la categoria no existe")
    void crear_categoria_inexistente_lanza_404() throws Exception {
        var yo = entrenador(1L, "Carlos");
        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":999,\"fecha\":\"2026-08-10\",\"horaInicio\":\"16:00:00\",\"horaFin\":\"17:00:00\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("hoy genera antes de leer, para que una sesion de horario fijo aparezca sin que nadie la cree a mano")
    void hoy_genera_las_sesiones_del_horario_fijo_antes_de_listar() throws Exception {
        autenticarComo("admin@sged.test", "ADMINISTRADOR");
        when(sesionRepository.findByFechaOrderByHoraInicioAsc(LocalDate.now())).thenReturn(List.of());

        mockMvc.perform(get("/api/sesiones/hoy")).andExpect(status().isOk());

        verify(horarioService).generarSesionesDeHoy();
    }

    // --- GET /api/sesiones/mias ---

    @Test
    @DisplayName("mias devuelve el historial completo del entrenador autenticado, no solo las de hoy")
    void mias_devuelve_historial_completo() throws Exception {
        var yo = entrenador(1L, "Carlos");
        var pasada = sesionDe(yo);
        pasada.setFecha(LocalDate.now().minusDays(4));

        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByEntrenadorIdEntrenadorOrderByFechaDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(pasada)));
        when(evaluacionRepository.existsBySesionIdSesion(any())).thenReturn(false);

        mockMvc.perform(get("/api/sesiones/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
