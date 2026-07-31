package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.seguridad.estado.dto.EstadoGeneralResponse;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.estado.service.EstadoGeneralService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadoGeneralServiceTest {

    @Mock
    private EstadoGeneralRepository estadoGeneralRepository;

    @InjectMocks
    private EstadoGeneralService estadoGeneralService;

    @Test
    @DisplayName("listarTodos mapea todas las entidades a EstadoGeneralResponse")
    void listarTodos_devuelve_todos_los_estados() {
        when(estadoGeneralRepository.findAll()).thenReturn(List.of(
                EstadoGeneral.builder().idEstadoGeneral(1L).nombre("ACTIVO").build(),
                EstadoGeneral.builder().idEstadoGeneral(2L).nombre("INACTIVO").build()
        ));

        List<EstadoGeneralResponse> resultado = estadoGeneralService.listarTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nombre()).isEqualTo("ACTIVO");
        assertThat(resultado.get(1).nombre()).isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("listarTodos devuelve lista vacia cuando no hay estados")
    void listarTodos_devuelve_vacio() {
        when(estadoGeneralRepository.findAll()).thenReturn(List.of());

        List<EstadoGeneralResponse> resultado = estadoGeneralService.listarTodos();

        assertThat(resultado).isEmpty();
    }
}
