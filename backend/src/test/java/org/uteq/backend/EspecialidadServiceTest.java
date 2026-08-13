package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadRequest;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadResponse;
import org.uteq.backend.deportivo.especialidad.entity.Especialidad;
import org.uteq.backend.deportivo.especialidad.repository.EspecialidadRepository;
import org.uteq.backend.deportivo.especialidad.service.EspecialidadService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspecialidadServiceTest {

    @Mock
    private EspecialidadRepository especialidadRepository;

    @InjectMocks
    private EspecialidadService especialidadService;

    private Especialidad tecnico() {
        return Especialidad.builder().idEspecialidad(1L).nombre("Técnico").activo(true).build();
    }

    @Test
    @DisplayName("listarPaginado delega en el repositorio y mapea a EspecialidadResponse")
    void listarPaginado_devuelve_pagina_mapeada() {
        Page<Especialidad> pagina = new PageImpl<>(List.of(tecnico()), PageRequest.of(0, 10), 1);
        when(especialidadRepository.findByActivoTrue(any())).thenReturn(pagina);

        Page<EspecialidadResponse> resultado = especialidadService.listarPaginado(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Técnico");
    }

    @Test
    @DisplayName("listarTodasActivas devuelve solo las especialidades activas")
    void listarTodasActivas_devuelve_lista() {
        when(especialidadRepository.findByActivoTrue()).thenReturn(List.of(tecnico()));

        List<EspecialidadResponse> resultado = especialidadService.listarTodasActivas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).idEspecialidad()).isEqualTo(1L);
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(especialidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> especialidadService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("crear persiste la especialidad cuando el nombre no existe")
    void crear_persiste_especialidad_valida() {
        EspecialidadRequest request = new EspecialidadRequest("Porteros");
        when(especialidadRepository.existsByNombreIgnoreCase("Porteros")).thenReturn(false);
        when(especialidadRepository.save(any(Especialidad.class))).thenAnswer(inv -> {
            Especialidad e = inv.getArgument(0);
            e.setIdEspecialidad(2L);
            return e;
        });

        EspecialidadResponse resultado = especialidadService.crear(request);

        assertThat(resultado.idEspecialidad()).isEqualTo(2L);
        assertThat(resultado.nombre()).isEqualTo("Porteros");
    }

    @Test
    @DisplayName("crear rechaza cuando ya existe una especialidad con ese nombre")
    void crear_con_nombre_duplicado_lanza_excepcion() {
        EspecialidadRequest request = new EspecialidadRequest("Técnico");
        when(especialidadRepository.existsByNombreIgnoreCase("Técnico")).thenReturn(true);

        assertThatThrownBy(() -> especialidadService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");

        verify(especialidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar actualiza el nombre de una especialidad existente")
    void editar_actualiza_especialidad() {
        Especialidad existente = tecnico();
        when(especialidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(especialidadRepository.save(any(Especialidad.class))).thenAnswer(inv -> inv.getArgument(0));

        EspecialidadRequest request = new EspecialidadRequest("Técnico avanzado");
        EspecialidadResponse resultado = especialidadService.editar(1L, request);

        assertThat(resultado.nombre()).isEqualTo("Técnico avanzado");
    }

    @Test
    @DisplayName("eliminar hace baja logica en vez de borrar el registro")
    void eliminar_hace_baja_logica() {
        Especialidad existente = tecnico();
        when(especialidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(especialidadRepository.save(any(Especialidad.class))).thenAnswer(inv -> inv.getArgument(0));

        especialidadService.eliminar(1L);

        assertThat(existente.getActivo()).isFalse();
        verify(especialidadRepository).save(existente);
    }
}
