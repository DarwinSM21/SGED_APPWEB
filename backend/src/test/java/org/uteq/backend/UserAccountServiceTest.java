package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.seguridad.auth.PasswordPolicy;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.role.entity.Role;
import org.uteq.backend.seguridad.role.repository.RoleRepository;
import org.uteq.backend.seguridad.user.dto.UserAccountPageResponse;
import org.uteq.backend.seguridad.user.dto.UserAccountRequest;
import org.uteq.backend.seguridad.user.dto.UserAccountResponse;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;
import org.uteq.backend.seguridad.user.service.UserAccountService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository usuarioRepository;
    @Mock
    private PersonRepository personaRepository;
    @Mock
    private GeneralStatusRepository estadoGeneralRepository;
    @Mock
    private RoleRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Spy
    private PasswordPolicy passwordPolicy = new PasswordPolicy();
    @Mock
    private EntrenadorRepository entrenadorRepository;
    @Mock
    private GuardianRepository representanteRepository;
    @Mock
    private StudentRepository estudianteRepository;

    @InjectMocks
    private UserAccountService usuarioService;

    private Person persona() {
        return Person.builder().id(1L).name("Ana").lastName("Torres")
                .email("ana@sged.test").build();
    }

    private GeneralStatus estadoActivo() {
        return GeneralStatus.builder().id(1L).name("ACTIVO").build();
    }

    private UserAccount usuario() {
        return UserAccount.builder()
                .id(1L)
                .person(persona())
                .generalStatus(estadoActivo())
                .username("ana.torres")
                .passwordHash("hash-existente")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("listar delega en el repositorio y mapea persona/estado")
    void listar_devuelve_pagina_mapeada() {
        Page<UserAccount> pagina = new PageImpl<>(List.of(usuario()), PageRequest.of(0, 10), 1);
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        UserAccountPageResponse<UserAccountResponse> resultado = usuarioService.list(PageRequest.of(0, 10));

        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content().get(0).username()).isEqualTo("ana.torres");
        assertThat(resultado.content().get(0).estadoGeneralNombre()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("listar incluye las cuentas desactivadas para que el administrador pueda reactivarlas")
    void listar_incluye_inactivos() {
        UserAccount apagado = usuario();
        apagado.setActive(false);
        Page<UserAccount> pagina = new PageImpl<>(List.of(apagado), PageRequest.of(0, 10), 1);
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        UserAccountPageResponse<UserAccountResponse> resultado = usuarioService.list(PageRequest.of(0, 10));

        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().get(0).activo()).isFalse();
    }

    @Test
    @DisplayName("reactivar vuelve a encender una cuenta apagada")
    void reactivar_enciende_la_cuenta() {
        UserAccount apagado = usuario();
        apagado.setActive(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(apagado));
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(i -> i.getArgument(0));

        UserAccountResponse resultado = usuarioService.reactivate(1L);

        assertThat(resultado.activo()).isTrue();
        assertThat(apagado.getActive()).isTrue();
    }

    @Test
    @DisplayName("reactivar rechaza una cuenta que ya estaba activa")
    void reactivar_rechaza_cuenta_ya_activa() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> usuarioService.reactivate(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra activa");

        verify(usuarioRepository, never()).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("reactivar lanza ResourceNotFoundException si el usuario no existe")
    void reactivar_usuario_inexistente() {
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.reactivate(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorId lanza ResourceNotFoundException cuando no existe o esta inactivo")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crear rechaza username duplicado")
    void crear_username_duplicado_lanza_excepcion() {
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "ana.torres", "clave123", null);
        when(usuarioRepository.existsByUsernameIgnoreCase("ana.torres")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra registrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear persiste el usuario con la contrasena codificada")
    void crear_persiste_usuario_valido() {
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "nuevo.usuario", "clave123", null);
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo.usuario")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(passwordEncoder.encode("clave123")).thenReturn("hash-codificado");
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserAccountResponse resultado = usuarioService.create(request);

        assertThat(resultado.idUsuario()).isEqualTo(2L);
        assertThat(resultado.username()).isEqualTo("nuevo.usuario");
        assertThat(resultado.roles()).isEmpty();
        verify(passwordEncoder).encode("clave123");
    }

    @Test
    @DisplayName("crear con rol lo busca y lo asigna al usuario nuevo")
    void crear_con_rol_asigna_el_rol() {
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "coach.nuevo", "clave123", "ENTRENADOR");
        Role entrenador = Role.builder().id(2L).name("ENTRENADOR").build();
        when(usuarioRepository.existsByUsernameIgnoreCase("coach.nuevo")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(rolRepository.findByNombre("ENTRENADOR")).thenReturn(Optional.of(entrenador));
        when(passwordEncoder.encode("clave123")).thenReturn("hash-codificado");
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });

        UserAccountResponse resultado = usuarioService.create(request);

        assertThat(resultado.roles()).containsExactly("ENTRENADOR");
    }

    @Test
    @DisplayName("crear con un rol inexistente lanza IllegalArgumentException")
    void crear_con_rol_inexistente_lanza_excepcion() {
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "nuevo", "clave123", "NO_EXISTE");
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(rolRepository.findByNombre("NO_EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rol inexistente");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear lanza ResourceNotFoundException si la persona no existe")
    void crear_persona_inexistente_lanza_excepcion() {
        UserAccountRequest request = new UserAccountRequest(99L, 1L, "nuevo", "clave123", null);
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo")).thenReturn(false);
        when(personaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("eliminar hace baja logica del usuario")
    void eliminar_hace_baja_logica() {
        UserAccount existente = usuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.delete(1L);

        assertThat(existente.getActive()).isFalse();
    }

    @Test
    @DisplayName("crear sin contrasena lanza IllegalArgumentException")
    void crear_sin_password_lanza_excepcion() {
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "nuevo", null, null);

        assertThatThrownBy(() -> usuarioService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contraseña es obligatoria");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar con contrasena en blanco no toca el hash existente")
    void editar_con_password_en_blanco_no_cambia_hash() {
        UserAccount existente = usuario();
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "ana.torres", null, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.update(1L, request);

        assertThat(existente.getPasswordHash()).isEqualTo("hash-existente");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("editar con contrasena nueva la re-hashea")
    void editar_con_password_nuevo_la_rehashea() {
        UserAccount existente = usuario();
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "ana.torres", "nuevaClave1", null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(passwordEncoder.encode("nuevaClave1")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.update(1L, request);

        assertThat(existente.getPasswordHash()).isEqualTo("hash-nuevo");
    }

    @Test
    @DisplayName("editar cambia el rol cuando la persona no tiene ninguna ficha activa")
    void editar_cambia_el_rol_sin_ficha_activa() {
        UserAccount existente = usuario();
        existente.setRoles(Set.of(Role.builder().id(1L).name("RECEPCIONISTA").build()));
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "ana.torres", null, "ENTRENADOR");
        Role entrenador = Role.builder().id(2L).name("ENTRENADOR").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(representanteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(rolRepository.findByNombre("ENTRENADOR")).thenReturn(Optional.of(entrenador));
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        UserAccountResponse resultado = usuarioService.update(1L, request);

        assertThat(resultado.roles()).containsExactly("ENTRENADOR");
    }

    @Test
    @DisplayName("editar rechaza el cambio de rol cuando la persona tiene ficha de entrenador activa")
    void editar_rechaza_cambio_de_rol_con_ficha_entrenador() {
        UserAccount existente = usuario();
        existente.setRoles(Set.of(Role.builder().id(1L).name("ENTRENADOR").build()));
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "ana.torres", null, "RECEPCIONISTA");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.update(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ficha de entrenador activa");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza un rol que no corresponde a la ficha de estudiante de la persona")
    void crear_con_rol_incoherente_con_ficha_estudiante_lanza_excepcion() {
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "fernanda.c", "clave123", "ENTRENADOR");
        when(usuarioRepository.existsByUsernameIgnoreCase("fernanda.c")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ficha de estudiante activa");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear acepta el rol que si corresponde a la ficha de la persona")
    void crear_con_rol_coherente_persiste() {
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "fernanda.c", "clave123", "ESTUDIANTE");
        Role estudiante = Role.builder().id(5L).name("ESTUDIANTE").build();
        when(usuarioRepository.existsByUsernameIgnoreCase("fernanda.c")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(true);
        when(entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(representanteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(estudiante));
        when(passwordEncoder.encode("clave123")).thenReturn("hash");
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        UserAccountResponse resultado = usuarioService.create(request);

        assertThat(resultado.roles()).containsExactly("ESTUDIANTE");
    }

    @Test
    @DisplayName("crear rechaza un rol que no corresponde a la ficha de representante de la persona")
    void crear_con_rol_incoherente_con_ficha_representante_lanza_excepcion() {
        UserAccountRequest request = new UserAccountRequest(1L, 1L, "ana.t", "clave123", "ADMINISTRADOR");
        when(usuarioRepository.existsByUsernameIgnoreCase("ana.t")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoActivo()));
        when(estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(entrenadorRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(false);
        when(representanteRepository.existsByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ficha de representante activa");

        verify(usuarioRepository, never()).save(any());
    }
}
