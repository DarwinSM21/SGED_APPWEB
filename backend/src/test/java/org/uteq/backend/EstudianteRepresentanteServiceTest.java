package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.estudianteRepresentante.dto.EstudianteRepresentanteRequest;
import org.uteq.backend.academico.estudianteRepresentante.dto.EstudianteRepresentanteResponse;
import org.uteq.backend.academico.estudianteRepresentante.entity.EstudianteRepresentante;
import org.uteq.backend.academico.estudianteRepresentante.repository.EstudianteRepresentanteRepository;
import org.uteq.backend.academico.estudianteRepresentante.service.EstudianteRepresentanteService;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstudianteRepresentanteServiceTest {

    @Mock private EstudianteRepresentanteRepository estudianteRepresentanteRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private RepresentanteRepository representanteRepository;

    @InjectMocks private EstudianteRepresentanteService service;

    private Estudiante estudianteDummy;
    private Representante representanteDummy;
    private EstudianteRepresentante relacionDummy;

    @BeforeEach
    void setUp() {
        Persona personaEstudiante = Persona.builder().idPersona(1L).nombre("Juan").apellido("Perez").build();
        Persona personaRepresentante = Persona.builder()
                .idPersona(2L).nombre("Rosa").apellido("Vera").telefono("0999999999").build();

        estudianteDummy = Estudiante.builder()
                .idEstudiante(1L)
                .persona(personaEstudiante)
                .codigoEstudiante("EST-001")
                .activo(true)
                .build();

        representanteDummy = Representante.builder()
                .idRepresentante(1L)
                .persona(personaRepresentante)
                .activo(true)
                .build();

        relacionDummy = EstudianteRepresentante.builder()
                .idEstudianteRepresentante(1L)
                .estudiante(estudianteDummy)
                .representante(representanteDummy)
                .relacion("Madre")
                .contactoPrincipal(true)
                .build();
    }

    private EstudianteRepresentanteRequest requestValido() {
        return new EstudianteRepresentanteRequest(1L, 1L, "Madre", true);
    }

    @Test
    @DisplayName("listar devuelve pagina mapeada")
    void listar_devuelve_pagina_mapeada() {
        Page<EstudianteRepresentante> pagina = new PageImpl<>(List.of(relacionDummy), PageRequest.of(0, 10), 1);
        when(estudianteRepresentanteRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        Page<EstudianteRepresentanteResponse> resultado = service.listar(PageRequest.of(0, 10));

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Madre", resultado.getContent().get(0).relacion());
    }

    @Test
    @DisplayName("listarPorEstudiante devuelve las relaciones del estudiante")
    void listarPorEstudiante_devuelve_lista() {
        when(estudianteRepresentanteRepository.findByEstudiante_IdEstudiante(1L)).thenReturn(List.of(relacionDummy));

        List<EstudianteRepresentanteResponse> resultado = service.listarPorEstudiante(1L);

        assertEquals(1, resultado.size());
        assertEquals("Rosa", resultado.get(0).nombreRepresentante());
    }

    @Test
    @DisplayName("buscarPorId lanza excepcion cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(estudianteRepresentanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    @Test
    @DisplayName("crear persiste la relacion cuando no existe previamente")
    void crear_persiste_relacion_valida() {
        when(estudianteRepresentanteRepository
                .existsByEstudiante_IdEstudianteAndRepresentante_IdRepresentante(1L, 1L)).thenReturn(false);
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representanteDummy));
        when(estudianteRepresentanteRepository.save(any(EstudianteRepresentante.class)))
                .thenAnswer(i -> i.getArgument(0));

        EstudianteRepresentanteResponse resultado = service.crear(requestValido());

        assertEquals("EST-001", resultado.codigoEstudiante());
        assertEquals("Madre", resultado.relacion());
        assertTrue(resultado.contactoPrincipal());
    }

    @Test
    @DisplayName("crear lanza excepcion si el representante ya esta vinculado al estudiante")
    void crear_relacion_duplicada_lanza_excepcion() {
        when(estudianteRepresentanteRepository
                .existsByEstudiante_IdEstudianteAndRepresentante_IdRepresentante(1L, 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.crear(requestValido()));
        verify(estudianteRepresentanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear usa contactoPrincipal=false cuando no se envia")
    void crear_contacto_principal_por_defecto_false() {
        when(estudianteRepresentanteRepository
                .existsByEstudiante_IdEstudianteAndRepresentante_IdRepresentante(1L, 1L)).thenReturn(false);
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representanteDummy));
        when(estudianteRepresentanteRepository.save(any(EstudianteRepresentante.class)))
                .thenAnswer(i -> i.getArgument(0));

        EstudianteRepresentanteResponse resultado =
                service.crear(new EstudianteRepresentanteRequest(1L, 1L, "Madre", null));

        assertFalse(resultado.contactoPrincipal());
    }

    @Test
    @DisplayName("eliminar borra fisicamente el registro (sin columna activo)")
    void eliminar_hace_borrado_fisico() {
        when(estudianteRepresentanteRepository.existsById(1L)).thenReturn(true);

        service.eliminar(1L);

        verify(estudianteRepresentanteRepository).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar lanza excepcion cuando no existe")
    void eliminar_inexistente_lanza_excepcion() {
        when(estudianteRepresentanteRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(99L));
        verify(estudianteRepresentanteRepository, never()).deleteById(any());
    }
}
