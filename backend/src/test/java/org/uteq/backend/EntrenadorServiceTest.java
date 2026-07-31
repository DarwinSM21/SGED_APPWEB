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
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorPageResponse;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorRequest;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorResponse;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.entrenador.service.EntrenadorService;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntrenadorServiceTest {

    @Mock
    private EntrenadorRepository entrenadorRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EntrenadorService entrenadorService;

    private Persona persona() {
        return Persona.builder().idPersona(1L).nombre("Carlos").apellido("Mora")
                .cedula("1234567890").correo("carlos@sged.test").build();
    }

    private Usuario usuario() {
        return Usuario.builder().idUsuario(1L).username("carlos.mora").build();
    }

    private Entrenador entrenador() {
        return Entrenador.builder()
                .idEntrenador(1L)
                .persona(persona())
                .usuario(usuario())
                .especialidad("Preparacion fisica")
                .experienciaAnios((short) 5)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("listar delega en el repositorio y mapea persona/usuario")
    void listar_devuelve_pagina_mapeada() {
        Page<Entrenador> pagina = new PageImpl<>(List.of(entrenador()), PageRequest.of(0, 10), 1);
        when(entrenadorRepository.findByActivoTrue(any())).thenReturn(pagina);

        EntrenadorPageResponse<EntrenadorResponse> resultado = entrenadorService.listar(PageRequest.of(0, 10));

        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content().get(0).nombre()).isEqualTo("Carlos");
        assertThat(resultado.content().get(0).username()).isEqualTo("carlos.mora");
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(entrenadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrenadorService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("crear rechaza cuando la persona ya es entrenador")
    void crear_persona_duplicada_lanza_excepcion() {
        EntrenadorRequest request = new EntrenadorRequest(1L, 2L, "Tactica", (short) 2, null);
        when(entrenadorRepository.existsByPersona_IdPersona(1L)).thenReturn(true);

        assertThatThrownBy(() -> entrenadorService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrada");

        verify(entrenadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza cuando el usuario ya esta asignado a otro entrenador")
    void crear_usuario_duplicado_lanza_excepcion() {
        EntrenadorRequest request = new EntrenadorRequest(1L, 2L, "Tactica", (short) 2, null);
        when(entrenadorRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(entrenadorRepository.existsByUsuario_IdUsuario(2L)).thenReturn(true);

        assertThatThrownBy(() -> entrenadorService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está asignado");
    }

    @Test
    @DisplayName("crear persiste el entrenador cuando persona y usuario son validos")
    void crear_persiste_entrenador_valido() {
        EntrenadorRequest request = new EntrenadorRequest(1L, 1L, "Fisica", (short) 3, "Cert-A");
        when(entrenadorRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(entrenadorRepository.existsByUsuario_IdUsuario(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(entrenadorRepository.save(any(Entrenador.class))).thenAnswer(inv -> {
            Entrenador e = inv.getArgument(0);
            e.setIdEntrenador(5L);
            return e;
        });

        EntrenadorResponse resultado = entrenadorService.crear(request);

        assertThat(resultado.idEntrenador()).isEqualTo(5L);
        assertThat(resultado.especialidad()).isEqualTo("Fisica");
    }

    @Test
    @DisplayName("eliminar hace baja logica del entrenador")
    void eliminar_hace_baja_logica() {
        Entrenador existente = entrenador();
        when(entrenadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(entrenadorRepository.save(any(Entrenador.class))).thenAnswer(inv -> inv.getArgument(0));

        entrenadorService.eliminar(1L);

        assertThat(existente.getActivo()).isFalse();
    }
}
