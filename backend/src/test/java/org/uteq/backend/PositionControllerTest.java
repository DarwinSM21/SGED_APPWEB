package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.deportivo.position.controller.PositionController;
import org.uteq.backend.deportivo.position.entity.Position;
import org.uteq.backend.deportivo.position.repository.PositionRepository;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PositionControllerTest {
    private MockMvc mockMvc;

    @Mock private PositionRepository positionRepository;

    @InjectMocks private PositionController posicionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(posicionController).build();
    }

    @Test
    @DisplayName("GET /api/posiciones/activas - mapea las posiciones activas a PositionResponse")
    void listarActivas_devuelve_las_posiciones() throws Exception {
        Position por = Position.builder().idPosicion(1L).nombre("Portero").abreviatura("POR").activo(true).build();
        Position dfc = Position.builder().idPosicion(2L).nombre("Defensa central").abreviatura("DFC").activo(true).build();
        when(positionRepository.findActiveOrderByIdAsc()).thenReturn(List.of(por, dfc));

        mockMvc.perform(get("/api/posiciones/activas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].idPosicion").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Portero"))
                .andExpect(jsonPath("$[0].abreviatura").value("POR"))
                .andExpect(jsonPath("$[1].abreviatura").value("DFC"));
    }

    @Test
    @DisplayName("GET /api/posiciones/activas - lista vacía devuelve 200 y []")
    void listarActivas_sin_posiciones_devuelve_lista_vacia() throws Exception {
        when(positionRepository.findActiveOrderByIdAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/posiciones/activas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }
}
