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
import org.uteq.backend.seguridad.estado.controller.EstadoGeneralController;
import org.uteq.backend.seguridad.estado.dto.EstadoGeneralResponse;
import org.uteq.backend.seguridad.estado.service.EstadoGeneralService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EstadoGeneralControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EstadoGeneralService estadoGeneralService;

    @InjectMocks
    private EstadoGeneralController estadoGeneralController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(estadoGeneralController).build();
    }

    @Test
    @DisplayName("GET /api/estados_generales - lista todos los estados")
    void listarTodos_devuelve_200() throws Exception {
        when(estadoGeneralService.listarTodos()).thenReturn(List.of(
                new EstadoGeneralResponse(1L, "ACTIVO"),
                new EstadoGeneralResponse(2L, "INACTIVO")
        ));

        mockMvc.perform(get("/api/estados_generales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("ACTIVO"))
                .andExpect(jsonPath("$[1].nombre").value("INACTIVO"));
    }
}
