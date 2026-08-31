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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.uteq.backend.seguridad.usuario.dto.UsuarioPageResponse;
import org.uteq.backend.seguridad.usuario.dto.UsuarioRequest;
import org.uteq.backend.seguridad.usuario.dto.UsuarioResponse;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;
import org.uteq.backend.seguridad.usuario.service.UsuarioService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EntrenadorRepository entrenadorRepository;
    @Mock
    private RepresentanteRepository representanteRepository;
    @Mock
    private EstudianteRepository estudianteRepository;

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
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        UsuarioPageResponse<UsuarioResponse> resultado = usuarioService.listar(PageRequest.of(0, 10));

        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content().get(0).username()).isEqualTo("ana.torres");
        assertThat(resultado.content().get(0).estadoGeneralNombre()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("listar incluye las cuentas desactivadas para que el administrador pueda reactivarlas")
    void listar_incluye_inactivos() {
        Usuario apagado = usuario();
        apagado.setActivo(false);
        Page<Usuario> pagina = new PageImpl<>(List.of(apagado), PageRequest.of(0, 10), 1);
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        UsuarioPageResponse<UsuarioResponse> resultado = usuarioService.listar(PageRequest.of(0, 10));

        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().get(0).activo()).isFalse();
    }

    @Test
    @DisplayName("reactivar vuelve a encender una cuenta apagada")
    void reactivar_enciende_la_cuenta() {
        Usuario apagado = usuario();
        apagado.setActivo(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(apagado));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse resultado = usuarioService.reactivar(1L);

        assertThat(resultado.activo()).isTrue();
        assertThat(apagado.getActivo()).isTrue();
    }

    @Test
    @DisplayName("reactivar rechaza una cuenta que ya estaba activa")
    void reactivar_rechaza_cuenta_ya_activa() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> usuarioService.reactivar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra activa");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("reactivar lanza RecursoNoEncontradoException si el usuario no existe")
    void reactivar_usuario_inexistente() {
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.reactivar(404L))
                .isInstanceOf(RecursoNoEncontradoException.class);
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
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "ana.torres", "clave123", null);
        when(usuarioRepository.existsByUsernameIgnoreCase("ana.torres")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra registrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear persiste el usuario con la contrasena codificada")
    void crear_persiste_usuario_valido() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "nuevo.usuario", "clave123", null);
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo.usuario")).thenReturn(false);
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
        assertThat(resultado.roles()).isEmpty();
        verify(passwordEncoder).encode("clave123");
    }

    @Test
    @DisplayName("crear con rol lo busca y lo asigna al usuario nuevo")
    void crear_con_rol_asigna_el_rol() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "coach.nuevo", "clave123", "ENTRENADOR");
        Rol entrenador = Rol.builder().idRol(2L).nombre("ENTRENADOR").build();
        when(usuarioRepository.existsByUsernameIgnoreCase("coach.nuevo")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(rolRepository.findByNombre("ENTRENADOR")).thenReturn(Optional.of(entrenador));
        when(passwordEncoder.encode("clave123")).thenReturn("hash-codificado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(3L);
            return u;
        });

        UsuarioResponse resultado = usuarioService.crear(request);

        assertThat(resultado.roles()).containsExactly("ENTRENADOR");
    }

    @Test
    @DisplayName("crear con un rol inexistente lanza IllegalArgumentException")
    void crear_con_rol_inexistente_lanza_excepcion() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "nuevo", "clave123", "NO_EXISTE");
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(rolRepository.findByNombre("NO_EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rol inexistente");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear lanza RecursoNoEncontradoException si la persona no existe")
    void crear_persona_inexistente_lanza_excepcion() {
        UsuarioRequest request = new UsuarioRequest(99L, 1L, "nuevo", "clave123", null);
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo")).thenReturn(false);
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

    @Test
    @DisplayName("crear sin contrasena lanza IllegalArgumentException")
    void crear_sin_password_lanza_excepcion() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "nuevo", null, null);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contraseña es obligatoria");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar con contrasena en blanco no toca el hash existente")
    void editar_con_password_en_blanco_no_cambia_hash() {
        Usuario existente = usuario();
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "ana.torres", null, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.editar(1L, request);

        assertThat(existente.getPassword_Hash()).isEqualTo("hash-existente");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("editar con contrasena nueva la re-hashea")
    void editar_con_password_nuevo_la_rehashea() {
        Usuario existente = usuario();
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "ana.torres", "nuevaClave1", null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(passwordEncoder.encode("nuevaClave1")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.editar(1L, request);

        assertThat(existente.getPassword_Hash()).isEqualTo("hash-nuevo");
    }

    @Test
    @DisplayName("editar cambia el rol cuando la persona no tiene ninguna ficha activa")
    void editar_cambia_el_rol_sin_ficha_activa() {
        Usuario existente = usuario();
        existente.setRoles(Set.of(Rol.builder().idRol(1L).nombre("RECEPCIONISTA").build()));
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "ana.torres", null, "ENTRENADOR");
        Rol entrenador = Rol.builder().idRol(2L).nombre("ENTRENADOR").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(representanteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(rolRepository.findByNombre("ENTRENADOR")).thenReturn(Optional.of(entrenador));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resultado = usuarioService.editar(1L, request);

        assertThat(resultado.roles()).containsExactly("ENTRENADOR");
    }

    @Test
    @DisplayName("editar rechaza el cambio de rol cuando la persona tiene ficha de entrenador activa")
    void editar_rechaza_cambio_de_rol_con_ficha_entrenador() {
        Usuario existente = usuario();
        existente.setRoles(Set.of(Rol.builder().idRol(1L).nombre("ENTRENADOR").build()));
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "ana.torres", null, "RECEPCIONISTA");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.editar(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ficha de entrenador activa");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza un rol que no corresponde a la ficha de estudiante de la persona")
    void crear_con_rol_incoherente_con_ficha_estudiante_lanza_excepcion() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "fernanda.c", "clave123", "ENTRENADOR");
        when(usuarioRepository.existsByUsernameIgnoreCase("fernanda.c")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ficha de estudiante activa");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear acepta el rol que si corresponde a la ficha de la persona")
    void crear_con_rol_coherente_persiste() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "fernanda.c", "clave123", "ESTUDIANTE");
        Rol estudiante = Rol.builder().idRol(5L).nombre("ESTUDIANTE").build();
        when(usuarioRepository.existsByUsernameIgnoreCase("fernanda.c")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(true);
        when(entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(representanteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(estudiante));
        when(passwordEncoder.encode("clave123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resultado = usuarioService.crear(request);

        assertThat(resultado.roles()).containsExactly("ESTUDIANTE");
    }

    @Test
    @DisplayName("crear rechaza un rol que no corresponde a la ficha de representante de la persona")
    void crear_con_rol_incoherente_con_ficha_representante_lanza_excepcion() {
        UsuarioRequest request = new UsuarioRequest(1L, 1L, "ana.t", "clave123", "ADMINISTRADOR");
        when(usuarioRepository.existsByUsernameIgnoreCase("ana.t")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(representanteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ficha de representante activa");

        verify(usuarioRepository, never()).save(any());
    }
}
