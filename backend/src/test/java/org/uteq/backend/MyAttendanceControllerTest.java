package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.deportivo.attendance.controller.MyAttendanceController;
import org.uteq.backend.deportivo.attendance.dto.AttendanceDtos.AttendanceResponse;
import org.uteq.backend.deportivo.attendance.dto.AttendanceDtos.MyHistoryResponse;
import org.uteq.backend.deportivo.attendance.service.AttendanceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MyAttendanceControllerTest {

    @Mock private AttendanceService attendanceService;

    @InjectMocks private MyAttendanceController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("miHistorial devuelve el historial del username autenticado, no de un id que el cliente pudiera mandar")
    void miHistorial_devuelve_200() throws Exception {
        autenticarComo("andres@sged.test");
        var asistencia = new AttendanceResponse(50L, LocalDate.of(2026, 8, 10), "SUB-12", LocalTime.of(16, 5), "PRESENTE");
        when(attendanceService.myAttendances("andres@sged.test"))
                .thenReturn(new MyHistoryResponse(List.of(asistencia), new BigDecimal("80.00")));

        mockMvc.perform(get("/api/estudiante/mi-asistencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asistencias", hasSize(1)))
                .andExpect(jsonPath("$.asistencias[0].categoria").value("SUB-12"))
                .andExpect(jsonPath("$.porcentajeUltimos30Dias").value(80.00));
    }
}
