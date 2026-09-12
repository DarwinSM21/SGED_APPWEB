package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.attendance.controller.SessionAttendanceController;
import org.uteq.backend.deportivo.attendance.dto.TakeAttendanceDtos.RosterResponse;
import org.uteq.backend.deportivo.attendance.service.AttendanceService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SessionAttendanceControllerTest {
    private MockMvc mockMvc;

    @Mock private AttendanceService attendanceService;

    @InjectMocks private SessionAttendanceController asistenciaSesionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(asistenciaSesionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RosterResponse roster() {
        return new RosterResponse(7L, "SUB-12", LocalDate.of(2026, 3, 10),
                LocalTime.of(16, 0), true, null, List.of());
    }

    @Test
    @DisplayName("GET /api/asistencias/sesion/{id} - devuelve la nómina de la sesión")
    void nomina_devuelve_200() throws Exception {
        when(attendanceService.roster(7L)).thenReturn(roster());

        mockMvc.perform(get("/api/asistencias/sesion/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSesion").value(7))
                .andExpect(jsonPath("$.categoria").value("SUB-12"))
                .andExpect(jsonPath("$.editable").value(true));
    }

    @Test
    @DisplayName("GET /api/asistencias/sesion/{id} - 404 si la sesión no existe")
    void nomina_sesion_inexistente_da_404() throws Exception {
        when(attendanceService.roster(99L)).thenThrow(new ResourceNotFoundException("Sesión no encontrada con id: 99"));

        mockMvc.perform(get("/api/asistencias/sesion/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/asistencias/sesion/{id} - pasa lista y devuelve la nómina actualizada")
    void pasarLista_devuelve_200() throws Exception {
        when(attendanceService.takeAttendance(eq(7L), any())).thenReturn(roster());

        mockMvc.perform(put("/api/asistencias/sesion/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marcas\":[{\"idEstudiante\":1,\"estado\":\"PRESENTE\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSesion").value(7));
    }

    @Test
    @DisplayName("PUT /api/asistencias/sesion/{id} - cuerpo sin marcas da 422")
    void pasarLista_sin_marcas_da_422() throws Exception {
        mockMvc.perform(put("/api/asistencias/sesion/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marcas\":[]}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
