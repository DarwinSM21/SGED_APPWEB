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
import org.uteq.backend.seguridad.auth.service.EmailVerificationService;
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

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private PersonService personaService;

    private Person persona() {
        return Person.builder()
                .id(1L)
                .name("Maria")
                .lastName("Lopez")
                .nationalId("1234567890")
                .email("maria@sged.test")
                .birthDate(LocalDate.of(2012, 5, 10))
                .active(true)
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
        when(personaRepository.findByActiveTrue(any())).thenReturn(pagina);

        Page<PersonResponse> resultado = personaService.list(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("buscarPorId lanza ResourceNotFoundException si esta inactiva o no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(personaRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personaService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorCedula devuelve la persona activa correspondiente")
    void buscarPorCedula_existente() {
        when(personaRepository.findByNationalIdAndActiveTrue("1234567890")).thenReturn(Optional.of(persona()));

        PersonResponse resultado = personaService.findByNationalId("1234567890");

        assertThat(resultado.cedula()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("crear rechaza cedula duplicada")
    void crear_cedula_duplicada_lanza_excepcion() {
        when(personaRepository.existsByNationalIdAndActiveTrue("1234567890")).thenReturn(true);

        assertThatThrownBy(() -> personaService.create(requestValido("1234567890", "nueva@sged.test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula");

        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza correo duplicado")
    void crear_correo_duplicado_lanza_excepcion() {
        when(personaRepository.existsByNationalIdAndActiveTrue("0000000000")).thenReturn(false);
        when(personaRepository.existsByEmail("maria@sged.test")).thenReturn(true);

        assertThatThrownBy(() -> personaService.create(requestValido("0000000000", "maria@sged.test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo");
    }

    @Test
    @DisplayName("crear persiste la persona cuando cedula y correo son unicos")
    void crear_persiste_persona_valida() {
        when(personaRepository.existsByNationalIdAndActiveTrue("0000000000")).thenReturn(false);
        when(personaRepository.existsByEmail("nueva@sged.test")).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> {
            Person p = inv.getArgument(0);
            p.setId(5L);
            return p;
        });

        PersonResponse resultado = personaService.create(requestValido("0000000000", "nueva@sged.test"));

        assertThat(resultado.idPersona()).isEqualTo(5L);
        assertThat(resultado.correo()).isEqualTo("nueva@sged.test");
    }

    @Test
    @DisplayName("RF-49 - crear sin cedula no consulta unicidad de cedula y persiste")
    void crear_sin_cedula_persiste() {
        when(personaRepository.existsByEmail("sincedula@sged.test")).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> {
            Person p = inv.getArgument(0);
            p.setId(9L);
            return p;
        });

        PersonResponse resultado = personaService.create(requestValido(null, "sincedula@sged.test"));

        assertThat(resultado.idPersona()).isEqualTo(9L);
        verify(personaRepository, never()).existsByNationalIdAndActiveTrue(any());
    }

    @Test
    @DisplayName("editar excluye a la propia persona al validar unicidad")
    void editar_actualiza_persona_existente() {
        Person existente = persona();
        when(personaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.existsAnotherPersonWithNationalId("1234567890", 1L)).thenReturn(false);
        when(personaRepository.existsAnotherPersonWithEmail("maria2@sged.test", 1L)).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        PersonResponse resultado = personaService.update(1L, requestValido("1234567890", "maria2@sged.test"));

        assertThat(resultado.correo()).isEqualTo("maria2@sged.test");
    }

    @Test
    @DisplayName("RNF-26 - crear deja el correo sin verificar y dispara el doble opt-in")
    void crear_dispara_confirmacion_de_correo() {
        when(personaRepository.existsByNationalIdAndActiveTrue("0000000000")).thenReturn(false);
        when(personaRepository.existsByEmail("nueva@sged.test")).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> {
            Person p = inv.getArgument(0);
            p.setId(5L);
            return p;
        });

        personaService.create(requestValido("0000000000", "nueva@sged.test"));

        org.mockito.ArgumentCaptor<Person> capturada = org.mockito.ArgumentCaptor.forClass(Person.class);
        verify(emailVerificationService).sendConfirmation(capturada.capture());
        assertThat(capturada.getValue().getEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("RNF-26 - editar sin cambiar el correo no dispara confirmacion ni lo invalida")
    void editar_sin_cambiar_correo_no_dispara_confirmacion() {
        Person existente = persona();
        existente.setEmailVerified(true);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.existsAnotherPersonWithNationalId("1234567890", 1L)).thenReturn(false);
        when(personaRepository.existsAnotherPersonWithEmail("maria@sged.test", 1L)).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        personaService.update(1L, requestValido("1234567890", "maria@sged.test"));

        verify(emailVerificationService, never()).sendConfirmation(any());
        assertThat(existente.getEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("RNF-26 - editar cambiando el correo lo invalida y dispara confirmacion")
    void editar_cambiando_correo_invalida_y_dispara() {
        Person existente = persona();
        existente.setEmailVerified(true);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.existsAnotherPersonWithNationalId("1234567890", 1L)).thenReturn(false);
        when(personaRepository.existsAnotherPersonWithEmail("otro@sged.test", 1L)).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        personaService.update(1L, requestValido("1234567890", "otro@sged.test"));

        verify(emailVerificationService).sendConfirmation(existente);
        assertThat(existente.getEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("eliminar hace baja logica de la persona")
    void eliminar_hace_baja_logica() {
        Person existente = persona();
        when(personaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        personaService.delete(1L);

        assertThat(existente.getActive()).isFalse();
    }
}
