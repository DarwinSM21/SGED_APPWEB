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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.deportivo.injury.controller.InjuryController;
import org.uteq.backend.deportivo.injury.entity.Injury;
import org.uteq.backend.deportivo.injury.service.InjuryService;
import org.uteq.backend.seguridad.person.entity.Person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InjuryControllerTest {
    @Mock private InjuryService injuryService;
    @Mock private CoachRepository coachRepository;

    @InjectMocks private InjuryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username, String rol) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PageImpl<Injury> paginaDe(Injury... lesiones) {
        var lista = List.of(lesiones);
        return new PageImpl<>(lista, PageRequest.of(0, 20), lista.size());
    }

    private Injury lesion(Long id, boolean activa) {
        var estudiante = Student.builder().id(1L)
                .person(Person.builder().name("Juan").lastName("Perez").build())
                .build();
        return Injury.builder()
                .idLesion(id)
                .estudiante(estudiante)
                .descripcion("Esguince de tobillo")
                .fechaLesion(LocalDate.of(2026, 8, 1))
                .fechaAlta(activa ? null : LocalDate.of(2026, 8, 15))
                .build();
    }

    @Test
    @DisplayName("listarActivas devuelve la pagina de lesiones sin alta")
    void listarActivas_devuelve_200() throws Exception {
        when(injuryService.findActive(any())).thenReturn(paginaDe(lesion(1L, true)));

        mockMvc.perform(get("/api/lesiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activa").value(true))
                .andExpect(jsonPath("$.content[0].estudiante").value("Juan Perez"));
    }

    @Test
    @DisplayName("historial devuelve la pagina de lesiones del estudiante, activas e inactivas")
    void historial_devuelve_200() throws Exception {
        when(injuryService.historyOf(eq(1L), any())).thenReturn(paginaDe(lesion(2L, false)));

        mockMvc.perform(get("/api/lesiones/estudiante/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activa").value(false))
                .andExpect(jsonPath("$.content[0].estudiante").value("Juan Perez"));
    }

    @Test
    @DisplayName("registrar devuelve 201 con la lesion activa recien creada (ADMINISTRADOR, con idEntrenador en el body)")
    void registrar_devuelve_201() throws Exception {
        autenticarComo("admin@sged.test", "ADMINISTRADOR");
        when(injuryService.register(eq(1L), eq(2L), eq("Esguince de tobillo"), any(), any()))
                .thenReturn(lesion(10L, true));

        mockMvc.perform(post("/api/lesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"idEntrenador\":2,\"descripcion\":\"Esguince de tobillo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activa").value(true))
                .andExpect(jsonPath("$.estudiante").value("Juan Perez"));
    }

    @Test
    @DisplayName("registrar propaga como 400 una segunda lesion activa del mismo estudiante")
    void registrar_segunda_lesion_activa_da_400() throws Exception {
        autenticarComo("admin@sged.test", "ADMINISTRADOR");
        when(injuryService.register(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException(
                        "El estudiante ya tiene una lesion activa registrada el 2026-08-01. Da de alta esa antes de registrar otra."));

        mockMvc.perform(post("/api/lesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"idEntrenador\":2,\"descripcion\":\"Otra lesion\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("registrar con descripcion vacia da 422 sin llegar al servicio")
    void registrar_descripcion_vacia_da_422() throws Exception {
        mockMvc.perform(post("/api/lesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"idEntrenador\":2,\"descripcion\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("registrar ignora el idEntrenador del body si quien llama es ENTRENADOR: usa el propio")
    void registrar_entrenador_ignora_idEntrenador_del_body() throws Exception {
        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(coachRepository.findByUserAccount_Username("carlos@sged.test"))
                .thenReturn(Optional.of(Coach.builder().idEntrenador(5L).build()));
        when(injuryService.register(eq(1L), eq(5L), eq("Esguince de tobillo"), any(), any()))
                .thenReturn(lesion(10L, true));

        mockMvc.perform(post("/api/lesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"idEntrenador\":999,\"descripcion\":\"Esguince de tobillo\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("registrar da 404 si la cuenta ENTRENADOR no tiene fila de entrenador asociada")
    void registrar_entrenador_sin_fila_asociada_da_404() throws Exception {
        autenticarComo("huerfano@sged.test", "ENTRENADOR");
        when(coachRepository.findByUserAccount_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/lesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"descripcion\":\"Esguince de tobillo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("registrar da 400 si ADMINISTRADOR no manda idEntrenador")
    void registrar_administrador_sin_idEntrenador_da_400() throws Exception {
        autenticarComo("admin@sged.test", "ADMINISTRADOR");

        mockMvc.perform(post("/api/lesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"descripcion\":\"Esguince de tobillo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("darDeAlta con fecha en el cuerpo cierra la lesion en esa fecha")
    void darDeAlta_con_cuerpo() throws Exception {
        when(injuryService.discharge(eq(10L), eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(lesion(10L, false));

        mockMvc.perform(post("/api/lesiones/10/alta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaAlta\":\"2026-08-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
    }

    @Test
    @DisplayName("darDeAlta sin cuerpo tambien funciona: el servicio usa la fecha de hoy")
    void darDeAlta_sin_cuerpo() throws Exception {
        when(injuryService.discharge(eq(10L), isNull())).thenReturn(lesion(10L, false));

        mockMvc.perform(post("/api/lesiones/10/alta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
    }

    @Test
    @DisplayName("darDeAlta propaga como 400 si la lesion ya tenia alta")
    void darDeAlta_ya_tenia_alta_da_400() throws Exception {
        when(injuryService.discharge(eq(10L), any()))
                .thenThrow(new IllegalArgumentException("Esa lesion ya tiene fecha de alta"));

        mockMvc.perform(post("/api/lesiones/10/alta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaAlta\":\"2026-08-15\"}"))
                .andExpect(status().isBadRequest());
    }
}
