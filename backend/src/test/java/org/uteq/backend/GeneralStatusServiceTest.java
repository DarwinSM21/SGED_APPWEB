package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.seguridad.status.dto.GeneralStatusResponse;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.status.service.GeneralStatusService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralStatusServiceTest {

    @Mock
    private GeneralStatusRepository estadoGeneralRepository;

    @InjectMocks
    private GeneralStatusService estadoGeneralService;

    @Test
    @DisplayName("listarTodos mapea todas las entidades a GeneralStatusResponse")
    void listarTodos_devuelve_todos_los_estados() {
        when(estadoGeneralRepository.findAll()).thenReturn(List.of(
                GeneralStatus.builder().id(1L).name("ACTIVO").build(),
                GeneralStatus.builder().id(2L).name("INACTIVO").build()
        ));

        List<GeneralStatusResponse> resultado = estadoGeneralService.findAll();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nombre()).isEqualTo("ACTIVO");
        assertThat(resultado.get(1).nombre()).isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("listarTodos devuelve lista vacia cuando no hay estados")
    void listarTodos_devuelve_vacio() {
        when(estadoGeneralRepository.findAll()).thenReturn(List.of());

        List<GeneralStatusResponse> resultado = estadoGeneralService.findAll();

        assertThat(resultado).isEmpty();
    }
}
