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
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.specialty.dto.SpecialtyRequest;
import org.uteq.backend.deportivo.specialty.dto.SpecialtyResponse;
import org.uteq.backend.deportivo.specialty.entity.Specialty;
import org.uteq.backend.deportivo.specialty.repository.SpecialtyRepository;
import org.uteq.backend.deportivo.specialty.service.SpecialtyService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private SpecialtyService specialtyService;

    private Specialty tecnico() {
        return Specialty.builder().idEspecialidad(1L).nombre("Técnico").activo(true).build();
    }

    @Test
    @DisplayName("listarPaginado delega en el repositorio y mapea a SpecialtyResponse")
    void listarPaginado_devuelve_pagina_mapeada() {
        Page<Specialty> pagina = new PageImpl<>(List.of(tecnico()), PageRequest.of(0, 10), 1);
        when(specialtyRepository.findActiveTrue(any())).thenReturn(pagina);

        Page<SpecialtyResponse> resultado = specialtyService.findPaged(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Técnico");
    }

    @Test
    @DisplayName("listarTodasActivas devuelve solo las especialidades activas")
    void listarTodasActivas_devuelve_lista() {
        when(specialtyRepository.findActiveTrue()).thenReturn(List.of(tecnico()));

        List<SpecialtyResponse> resultado = specialtyService.findAllActive();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).idEspecialidad()).isEqualTo(1L);
    }

    @Test
    @DisplayName("buscarPorId lanza ResourceNotFoundException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crear persiste la especialidad cuando el nombre no existe")
    void crear_persiste_especialidad_valida() {
        SpecialtyRequest request = new SpecialtyRequest("Porteros");
        when(specialtyRepository.existsByNameIgnoreCase("Porteros")).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(inv -> {
            Specialty e = inv.getArgument(0);
            e.setIdEspecialidad(2L);
            return e;
        });

        SpecialtyResponse resultado = specialtyService.create(request);

        assertThat(resultado.idEspecialidad()).isEqualTo(2L);
        assertThat(resultado.nombre()).isEqualTo("Porteros");
    }

    @Test
    @DisplayName("crear rechaza cuando ya existe una especialidad con ese nombre")
    void crear_con_nombre_duplicado_lanza_excepcion() {
        SpecialtyRequest request = new SpecialtyRequest("Técnico");
        when(specialtyRepository.existsByNameIgnoreCase("Técnico")).thenReturn(true);

        assertThatThrownBy(() -> specialtyService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");

        verify(specialtyRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar actualiza el nombre de una especialidad existente")
    void editar_actualiza_especialidad() {
        Specialty existente = tecnico();
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        SpecialtyRequest request = new SpecialtyRequest("Técnico avanzado");
        SpecialtyResponse resultado = specialtyService.update(1L, request);

        assertThat(resultado.nombre()).isEqualTo("Técnico avanzado");
    }

    @Test
    @DisplayName("eliminar hace baja logica en vez de borrar el registro")
    void eliminar_hace_baja_logica() {
        Specialty existente = tecnico();
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        specialtyService.delete(1L);

        assertThat(existente.getActivo()).isFalse();
        verify(specialtyRepository).save(existente);
    }
}
