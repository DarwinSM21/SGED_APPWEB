package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.seguridad.audit.controller.AuditController;
import org.uteq.backend.seguridad.audit.dto.AuditLogResponse;
import org.uteq.backend.seguridad.audit.service.AuditService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {
    @Mock private AuditService auditoriaService;
    @InjectMocks private AuditController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuditLogResponse fila() {
        return new AuditLogResponse(1L, OffsetDateTime.now(), "ana.torres", "ADMINISTRADOR",
                "EDITAR", "Injury", 45L, "editó Injury #45");
    }

    @Test
    @DisplayName("GET /api/admin/auditorias sin filtros devuelve la pagina completa")
    void listarSinFiltros() throws Exception {
        when(auditoriaService.search(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(fila()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/auditorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].usuario").value("ana.torres"))
                .andExpect(jsonPath("$.content[0].accion").value("EDITAR"))
                .andExpect(jsonPath("$.content[0].entidad").value("Injury"));
    }

    @Test
    @DisplayName("GET /api/admin/auditorias reenvia los filtros de query al servicio")
    void listarConFiltros() throws Exception {
        when(auditoriaService.search(eq("ana"), eq("EDITAR"), eq("Injury"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(fila()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/auditorias")
                        .param("usuario", "ana")
                        .param("accion", "EDITAR")
                        .param("entidad", "Injury")
                        .param("fechaDesde", "2026-08-01")
                        .param("fechaHasta", "2026-08-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descripcion").value("editó Injury #45"));
    }
}
