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
import org.uteq.backend.seguridad.person.dto.PersonRequest;
import org.uteq.backend.seguridad.person.dto.PersonResponse;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.person.service.PersonService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personaRepository;

    @InjectMocks
    private PersonService personaService;

    private Person persona() {
        return Person.builder()
                .idPersona(1L)
                .nombre("Maria")
                .apellido("Lopez")
                .cedula("1234567890")
                .correo("maria@sged.test")
                .fechaNacimiento(LocalDate.of(2012, 5, 10))
                .activo(true)
                .build();
    }

    private PersonRequest requestValido(String cedula, String correo) {
        return new PersonRequest("Maria", "Lopez", cedula, correo, "0999999999", null,
                LocalDate.of(2012, 5, 10));
    }

    @Test
    @DisplayName("listar delega en el repositorio")
    void listar_devuelve_pagina() {
        Page<Person> pagina = new PageImpl<>(List.of(persona()), PageRequest.of(0, 10), 1);
        when(personaRepository.findByActivoTrue(any())).thenReturn(pagina);

        Page<PersonResponse> resultado = personaService.list(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("buscarPorId lanza ResourceNotFoundException si esta inactiva o no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(personaRepository.findByIdPersonaAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personaService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorCedula devuelve la persona activa correspondiente")
    void buscarPorCedula_existente() {
        when(personaRepository.findByCedulaAndActivoTrue("1234567890")).thenReturn(Optional.of(persona()));

        PersonResponse resultado = personaService.findByCedula("1234567890");

        assertThat(resultado.cedula()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("crear rechaza cedula duplicada")
    void crear_cedula_duplicada_lanza_excepcion() {
        when(personaRepository.existsByCedulaAndActivoTrue("1234567890")).thenReturn(true);

        assertThatThrownBy(() -> personaService.create(requestValido("1234567890", "nueva@sged.test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula");

        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza correo duplicado")
    void crear_correo_duplicado_lanza_excepcion() {
        when(personaRepository.existsByCedulaAndActivoTrue("0000000000")).thenReturn(false);
        when(personaRepository.existsByCorreo("maria@sged.test")).thenReturn(true);

        assertThatThrownBy(() -> personaService.create(requestValido("0000000000", "maria@sged.test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo");
    }

    @Test
    @DisplayName("crear persiste la persona cuando cedula y correo son unicos")
    void crear_persiste_persona_valida() {
        when(personaRepository.existsByCedulaAndActivoTrue("0000000000")).thenReturn(false);
        when(personaRepository.existsByCorreo("nueva@sged.test")).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> {
            Person p = inv.getArgument(0);
            p.setIdPersona(5L);
            return p;
        });

        PersonResponse resultado = personaService.create(requestValido("0000000000", "nueva@sged.test"));

        assertThat(resultado.idPersona()).isEqualTo(5L);
        assertThat(resultado.correo()).isEqualTo("nueva@sged.test");
    }

    @Test
    @DisplayName("editar excluye a la propia persona al validar unicidad")
    void editar_actualiza_persona_existente() {
        Person existente = persona();
        when(personaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.existsAnotherPersonWithCedula("1234567890", 1L)).thenReturn(false);
        when(personaRepository.existsAnotherPersonWithEmail("maria2@sged.test", 1L)).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        PersonResponse resultado = personaService.update(1L, requestValido("1234567890", "maria2@sged.test"));

        assertThat(resultado.correo()).isEqualTo("maria2@sged.test");
    }

    @Test
    @DisplayName("eliminar hace baja logica de la persona")
    void eliminar_hace_baja_logica() {
        Person existente = persona();
        when(personaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        personaService.delete(1L);

        assertThat(existente.getActivo()).isFalse();
    }
}
