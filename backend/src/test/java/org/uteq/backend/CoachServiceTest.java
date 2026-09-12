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
import org.springframework.data.domain.Pageable;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.coach.dto.CoachPageResponse;
import org.uteq.backend.deportivo.coach.dto.CoachRequest;
import org.uteq.backend.deportivo.coach.dto.CoachResponse;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.deportivo.coach.service.CoachService;
import org.uteq.backend.deportivo.specialty.entity.Specialty;
import org.uteq.backend.deportivo.specialty.repository.SpecialtyRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.role.entity.Role;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoachServiceTest {

    @Mock
    private CoachRepository coachRepository;
    @Mock
    private PersonRepository personaRepository;
    @Mock
    private UserAccountRepository usuarioRepository;
    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private CoachService coachService;

    private Person persona() {
        return Person.builder().id(1L).name("Carlos").lastName("Mora")
                .nationalId("1234567890").email("carlos@sged.test").build();
    }

    private UserAccount usuario() {
        return UserAccount.builder().id(1L).username("carlos.mora")
                .roles(Set.of(Role.builder().id(1L).name("ENTRENADOR").build())).build();
    }

    private Specialty especialidad() {
        return Specialty.builder().idEspecialidad(1L).nombre("Preparador físico").activo(true).build();
    }

    private Coach entrenador() {
        return Coach.builder()
                .idEntrenador(1L)
                .persona(persona())
                .usuario(usuario())
                .especialidad(especialidad())
                .experienciaAnios((short) 5)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("listar delega en el repositorio y mapea persona/usuario")
    void listar_devuelve_pagina_mapeada() {
        Page<Coach> pagina = new PageImpl<>(List.of(entrenador()), PageRequest.of(0, 10), 1);
        when(coachRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        CoachPageResponse<CoachResponse> resultado = coachService.list(PageRequest.of(0, 10));

        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content().get(0).nombre()).isEqualTo("Carlos");
        assertThat(resultado.content().get(0).username()).isEqualTo("carlos.mora");
    }

    @Test
    @DisplayName("buscarPorId lanza ResourceNotFoundException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(coachRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coachService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crear rechaza cuando la persona ya es entrenador")
    void crear_persona_duplicada_lanza_excepcion() {
        CoachRequest request = new CoachRequest(1L, 2L, 1L, (short) 2, null);
        when(coachRepository.existsByPerson_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> coachService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrada");

        verify(coachRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza cuando el usuario ya esta asignado a otro entrenador")
    void crear_usuario_duplicado_lanza_excepcion() {
        CoachRequest request = new CoachRequest(1L, 2L, 1L, (short) 2, null);
        when(coachRepository.existsByPerson_Id(1L)).thenReturn(false);
        when(coachRepository.existsByUserAccount_Id(2L)).thenReturn(true);

        assertThatThrownBy(() -> coachService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está asignado");
    }

    @Test
    @DisplayName("crear persiste el entrenador cuando persona y usuario son validos")
    void crear_persiste_entrenador_valido() {
        CoachRequest request = new CoachRequest(1L, 1L, 1L, (short) 3, "Cert-A");
        when(coachRepository.existsByPerson_Id(1L)).thenReturn(false);
        when(coachRepository.existsByUserAccount_Id(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(especialidad()));
        when(coachRepository.save(any(Coach.class))).thenAnswer(inv -> {
            Coach e = inv.getArgument(0);
            e.setIdEntrenador(5L);
            return e;
        });

        CoachResponse resultado = coachService.create(request);

        assertThat(resultado.idEntrenador()).isEqualTo(5L);
        assertThat(resultado.nombreEspecialidad()).isEqualTo("Preparador físico");
    }

    @Test
    @DisplayName("crear rechaza cuando la especialidad indicada no existe")
    void crear_especialidad_inexistente_lanza_excepcion() {
        CoachRequest request = new CoachRequest(1L, 1L, 99L, (short) 3, null);
        when(coachRepository.existsByPerson_Id(1L)).thenReturn(false);
        when(coachRepository.existsByUserAccount_Id(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coachService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crear rechaza cuando el usuario no tiene el rol ENTRENADOR")
    void crear_sin_rol_entrenador_lanza_excepcion() {
        CoachRequest request = new CoachRequest(1L, 1L, null, (short) 3, null);
        UserAccount usuarioSinRol = UserAccount.builder().id(1L).username("carlos.mora")
                .roles(Set.of(Role.builder().id(2L).name("RECEPCIONISTA").build())).build();
        when(coachRepository.existsByPerson_Id(1L)).thenReturn(false);
        when(coachRepository.existsByUserAccount_Id(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioSinRol));

        assertThatThrownBy(() -> coachService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rol ENTRENADOR");

        verify(coachRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar hace baja logica del entrenador")
    void eliminar_hace_baja_logica() {
        Coach existente = entrenador();
        when(coachRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(coachRepository.save(any(Coach.class))).thenAnswer(inv -> inv.getArgument(0));

        coachService.delete(1L);

        assertThat(existente.getActivo()).isFalse();
    }
}
