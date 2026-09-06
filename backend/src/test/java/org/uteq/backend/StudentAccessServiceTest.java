package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uteq.backend.academico.student.dto.EnableAccessRequest;
import org.uteq.backend.academico.student.service.StudentAccessService;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.role.entity.Role;
import org.uteq.backend.seguridad.role.repository.RoleRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAccessServiceTest {
    @Mock private UserAccountRepository usuarioRepository;
    @Mock private RoleRepository rolRepository;
    @Mock private GeneralStatusRepository estadoGeneralRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private StudentAccessService service;

    private final Person persona = Person.builder().idPersona(1L).nombre("Ana").apellido("Vera").build();

    @Test
    @DisplayName("validarCoherenciaConFichaEstudiante no lanza si la persona no tiene cuenta")
    void validarCoherencia_sin_cuenta_no_lanza() {
        when(usuarioRepository.findByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(Optional.empty());

        service.validateConsistencyWithStudentRecord(1L);
    }

    @Test
    @DisplayName("validarCoherenciaConFichaEstudiante lanza si la cuenta existente es de otro rol")
    void validarCoherencia_con_cuenta_de_otro_rol_lanza() {
        UserAccount cuentaEntrenador = UserAccount.builder().idUsuario(9L)
                .roles(Set.of(Role.builder().idRol(2L).nombre("ENTRENADOR").build())).build();
        when(usuarioRepository.findByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(Optional.of(cuentaEntrenador));

        assertThatThrownBy(() -> service.validateConsistencyWithStudentRecord(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validarCoherenciaConFichaEstudiante no lanza si la cuenta ya es de rol ESTUDIANTE")
    void validarCoherencia_con_cuenta_de_estudiante_no_lanza() {
        UserAccount cuentaEstudiante = UserAccount.builder().idUsuario(9L)
                .roles(Set.of(Role.builder().idRol(5L).nombre("ESTUDIANTE").build())).build();
        when(usuarioRepository.findByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(Optional.of(cuentaEstudiante));

        service.validateConsistencyWithStudentRecord(1L);
    }

    @Test
    @DisplayName("crearCuentaDeEstudiante rechaza un username ya en uso, sin guardar nada")
    void crearCuenta_username_duplicado_lanza() {
        when(usuarioRepository.existsByUsernameIgnoreCase("dup@sged.test")).thenReturn(true);

        assertThatThrownBy(() -> service.createStudentAccount(
                persona, new EnableAccessRequest("dup@sged.test", "password123")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearCuentaDeEstudiante crea el usuario con rol ESTUDIANTE, la contrasena hasheada y sobre la Persona dada")
    void crearCuenta_exitosa() {
        EnableAccessRequest request = new EnableAccessRequest("andres@sged.test", "password123");
        Role rolEstudiante = Role.builder().idRol(6L).nombre("ESTUDIANTE").build();

        when(usuarioRepository.existsByUsernameIgnoreCase("andres@sged.test")).thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(rolEstudiante));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(GeneralStatus.builder().idEstadoGeneral(1L).build()));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$encoded");
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(i -> {
            UserAccount u = i.getArgument(0);
            u.setIdUsuario(9L);
            return u;
        });

        UserAccount resultado = service.createStudentAccount(persona, request);

        assertThat(resultado.getIdUsuario()).isEqualTo(9L);
        assertThat(resultado.getPersona()).isSameAs(persona);
        assertThat(resultado.getPassword_Hash()).isEqualTo("$2a$12$encoded");
        assertThat(resultado.getRoles()).containsExactly(rolEstudiante);
    }

    @Test
    @DisplayName("crearCuentaDeEstudiante lanza IllegalStateException si falta el rol ESTUDIANTE en el catalogo")
    void crearCuenta_sin_rol_estudiante_en_catalogo_lanza() {
        when(usuarioRepository.existsByUsernameIgnoreCase("x@sged.test")).thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createStudentAccount(persona, new EnableAccessRequest("x@sged.test", "password123")))
                .isInstanceOf(IllegalStateException.class);
    }
}
