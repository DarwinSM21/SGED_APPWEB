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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.dto.UsuarioPageResponse;
import org.uteq.backend.seguridad.usuario.dto.UsuarioRequest;
import org.uteq.backend.seguridad.usuario.dto.UsuarioResponse;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;
import org.uteq.backend.seguridad.usuario.service.UsuarioService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private EstadoGeneralRepository estadoGeneralRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Persona persona() {
        return Persona.builder().idPersona(1L).nombre("Ana").apellido("Torres")
                .correo("ana@sged.test").build();
    }

    private EstadoGeneral estadoActivo() {
        return EstadoGeneral.builder().idEstadoGeneral(1L).nombre("ACTIVO").build();
    }

    private Usuario usuario() {
        return Usuario.builder()
                .idUsuario(1L)
                .persona(persona())
                .estadoGeneral(estadoActivo())
                .username("ana.torres")
                .password_Hash("hash-existente")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("listar delega en el repositorio y mapea persona/estado")
    void listar_devuelve_pagina_mapeada() {
        Page<Usuario> pagina = new PageImpl<>(List.of(usuario()), PageRequest.of(0, 10), 1);
        when(usuarioRepository.findByActivoTrue(any())).thenReturn(pagina);

        UsuarioPageResponse<UsuarioResponse> resultado = usuarioService.listar(PageRequest.of(0, 10));

        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content().get(0).username()).isEqualTo("ana.torres");
        assertThat(resultado.content().get(0).estadoGeneralNombre()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando no existe o esta inactivo")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("crear rechaza username duplicado")
    void crear_username_duplicado_lanza_excepcion() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "ana.torres", "clave123");
        when(usuarioRepository.existsByUsername("ana.torres")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra registrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear persiste el usuario con la contrasena codificada")
    void crear_persiste_usuario_valido() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "nuevo.usuario", "clave123");
        when(usuarioRepository.existsByUsername("nuevo.usuario")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(passwordEncoder.encode("clave123")).thenReturn("hash-codificado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(2L);
            return u;
        });

        UsuarioResponse resultado = usuarioService.crear(request);

        assertThat(resultado.idUsuario()).isEqualTo(2L);
        assertThat(resultado.username()).isEqualTo("nuevo.usuario");
        verify(passwordEncoder).encode("clave123");
    }

    @Test
    @DisplayName("crear lanza RecursoNoEncontradoException si la persona no existe")
    void crear_persona_inexistente_lanza_excepcion() {
        UsuarioRequest request = new UsuarioRequest(99L, 1L, "nuevo", "clave123");
        when(usuarioRepository.existsByUsername("nuevo")).thenReturn(false);
        when(personaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("eliminar hace baja logica del usuario")
    void eliminar_hace_baja_logica() {
        Usuario existente = usuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.eliminar(1L);

        assertThat(existente.getActivo()).isFalse();
    }
}
