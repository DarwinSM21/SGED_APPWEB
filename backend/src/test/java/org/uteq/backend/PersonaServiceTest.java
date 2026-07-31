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
import org.uteq.backend.seguridad.persona.dto.PersonaRequest;
import org.uteq.backend.seguridad.persona.dto.PersonaResponse;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.persona.service.PersonaService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @InjectMocks
    private PersonaService personaService;

    private Persona persona() {
        return Persona.builder()
                .idPersona(1L)
                .nombre("Maria")
                .apellido("Lopez")
                .cedula("1234567890")
                .correo("maria@sged.test")
                .fechaNacimiento(LocalDate.of(2012, 5, 10))
                .activo(true)
                .build();
    }

    private PersonaRequest requestValido(String cedula, String correo) {
        return new PersonaRequest("Maria", "Lopez", cedula, correo, "0999999999", null,
                LocalDate.of(2012, 5, 10));
    }

    @Test
    @DisplayName("listar delega en el repositorio")
    void listar_devuelve_pagina() {
        Page<Persona> pagina = new PageImpl<>(List.of(persona()), PageRequest.of(0, 10), 1);
        when(personaRepository.findByActivoTrue(any())).thenReturn(pagina);

        Page<PersonaResponse> resultado = personaService.listar(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException si esta inactiva o no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(personaRepository.findByIdPersonaAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personaService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("buscarPorCedula devuelve la persona activa correspondiente")
    void buscarPorCedula_existente() {
        when(personaRepository.findByCedulaAndActivoTrue("1234567890")).thenReturn(Optional.of(persona()));

        PersonaResponse resultado = personaService.buscarPorCedula("1234567890");

        assertThat(resultado.cedula()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("crear rechaza cedula duplicada")
    void crear_cedula_duplicada_lanza_excepcion() {
        when(personaRepository.existsByCedulaAndActivoTrue("1234567890")).thenReturn(true);

        assertThatThrownBy(() -> personaService.crear(requestValido("1234567890", "nueva@sged.test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula");

        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza correo duplicado")
    void crear_correo_duplicado_lanza_excepcion() {
        when(personaRepository.existsByCedulaAndActivoTrue("0000000000")).thenReturn(false);
        when(personaRepository.existsByCorreo("maria@sged.test")).thenReturn(true);

        assertThatThrownBy(() -> personaService.crear(requestValido("0000000000", "maria@sged.test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo");
    }

    @Test
    @DisplayName("crear persiste la persona cuando cedula y correo son unicos")
    void crear_persiste_persona_valida() {
        when(personaRepository.existsByCedulaAndActivoTrue("0000000000")).thenReturn(false);
        when(personaRepository.existsByCorreo("nueva@sged.test")).thenReturn(false);
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> {
            Persona p = inv.getArgument(0);
            p.setIdPersona(5L);
            return p;
        });

        PersonaResponse resultado = personaService.crear(requestValido("0000000000", "nueva@sged.test"));

        assertThat(resultado.idPersona()).isEqualTo(5L);
        assertThat(resultado.correo()).isEqualTo("nueva@sged.test");
    }

    @Test
    @DisplayName("editar excluye a la propia persona al validar unicidad")
    void editar_actualiza_persona_existente() {
        Persona existente = persona();
        when(personaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.existeOtraPersonaConCedula("1234567890", 1L)).thenReturn(false);
        when(personaRepository.existeOtraPersonaConCorreo("maria2@sged.test", 1L)).thenReturn(false);
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        PersonaResponse resultado = personaService.editar(1L, requestValido("1234567890", "maria2@sged.test"));

        assertThat(resultado.correo()).isEqualTo("maria2@sged.test");
    }

    @Test
    @DisplayName("eliminar hace baja logica de la persona")
    void eliminar_hace_baja_logica() {
        Persona existente = persona();
        when(personaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        personaService.eliminar(1L);

        assertThat(existente.getActivo()).isFalse();
    }
}
