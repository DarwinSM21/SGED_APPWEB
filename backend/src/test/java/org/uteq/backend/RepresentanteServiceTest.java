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
import org.uteq.backend.academico.representante.dto.RepresentanteRequest;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.academico.representante.service.RepresentanteService;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepresentanteServiceTest {

    @Mock private RepresentanteRepository representanteRepository;
    @Mock private PersonaRepository personaRepository;

    @InjectMocks private RepresentanteService service;

    private Persona personaDummy;
    private Representante representanteDummy;

    @BeforeEach
    void setUp() {
        personaDummy = Persona.builder()
                .idPersona(1L)
                .nombre("Rosa")
                .apellido("Vera")
                .telefono("0999999999")
                .activo(true)
                .build();

        representanteDummy = Representante.builder()
                .idRepresentante(1L)
                .persona(personaDummy)
                .ocupacion("Comerciante")
                .activo(true)
                .build();
    }

    private RepresentanteRequest requestValido() {
        return new RepresentanteRequest(1L, "Comerciante");
    }

    @Test
    @DisplayName("listar devuelve pagina mapeada de representantes activos")
    void listar_devuelve_pagina_mapeada() {
        Page<Representante> pagina = new PageImpl<>(List.of(representanteDummy), PageRequest.of(0, 10), 1);
        when(representanteRepository.findByActivoTrue(any())).thenReturn(pagina);

        Page<RepresentanteResponse> resultado = service.listar(PageRequest.of(0, 10));

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Rosa", resultado.getContent().get(0).nombrePersona());
    }

    @Test
    @DisplayName("buscarPorId devuelve el representante cuando existe")
    void buscarPorId_existente() {
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representanteDummy));

        RepresentanteResponse resultado = service.buscarPorId(1L);

        assertEquals(1L, resultado.idRepresentante());
        assertEquals("Comerciante", resultado.ocupacion());
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(representanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    @Test
    @DisplayName("crear persiste el representante cuando la persona no tiene ficha previa")
    void crear_persiste_representante_valido() {
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(personaDummy));
        when(representanteRepository.save(any(Representante.class))).thenAnswer(i -> i.getArgument(0));

        RepresentanteResponse resultado = service.crear(requestValido());

        assertEquals("Rosa", resultado.nombrePersona());
        assertTrue(resultado.activo());
        verify(representanteRepository).save(any(Representante.class));
    }

    @Test
    @DisplayName("crear lanza excepcion si la persona ya es representante")
    void crear_persona_ya_representante_lanza_excepcion() {
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.crear(requestValido()));
        verify(representanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear lanza excepcion si la persona no existe")
    void crear_persona_inexistente_lanza_excepcion() {
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(requestValido()));
    }

    @Test
    @DisplayName("editar actualiza la ocupacion del representante")
    void editar_actualiza_representante() {
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representanteDummy));
        when(representanteRepository.save(any(Representante.class))).thenAnswer(i -> i.getArgument(0));

        RepresentanteResponse resultado = service.editar(1L, new RepresentanteRequest(1L, "Ingeniero"));

        assertEquals("Ingeniero", resultado.ocupacion());
    }

    @Test
    @DisplayName("editar lanza excepcion si la nueva persona ya es representante")
    void editar_reasigna_persona_ya_representante_lanza_excepcion() {
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representanteDummy));
        when(representanteRepository.existsByPersona_IdPersona(2L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.editar(1L, new RepresentanteRequest(2L, "Ingeniero")));
    }

    @Test
    @DisplayName("eliminar hace baja logica en vez de borrar el registro")
    void eliminar_hace_baja_logica() {
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representanteDummy));
        when(representanteRepository.save(any(Representante.class))).thenAnswer(i -> i.getArgument(0));

        service.eliminar(1L);

        assertFalse(representanteDummy.getActivo());
        verify(representanteRepository).save(representanteDummy);
    }
}
