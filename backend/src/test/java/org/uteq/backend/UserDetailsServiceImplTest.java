package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.uteq.backend.seguridad.auth.security.UserDetailsServiceImpl;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Usuario usuarioDe(String username) {
        return Usuario.builder()
                .idUsuario(1L)
                .username(username)
                .password_Hash("$2b$12$hashfalso")
                .activo(true)
                .persona(Persona.builder().idPersona(1L).nombre("Juan").apellido("Perez").build())
                .estadoGeneral(EstadoGeneral.builder().idEstadoGeneral(1L).nombre("Activo").build())
                .roles(Set.of(Rol.builder().idRol(1L).nombre("ESTUDIANTE").build()))
                .build();
    }

    @Test
    @DisplayName("un username escrito con mayusculas encuentra la misma cuenta")
    void usernameConMayusculasEncuentraLaCuenta() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("Juan.Perez@sged.test"))
                .thenReturn(Optional.of(usuarioDe("juan.perez@sged.test")));

        UserDetails detalles = userDetailsService.loadUserByUsername("Juan.Perez@sged.test");

        assertThat(detalles).isNotNull();
    }

    @Test
    @DisplayName("el principal queda con el username canonico guardado, no con lo que se tecleo")
    void elPrincipalUsaElUsernameGuardado() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue(anyString()))
                .thenReturn(Optional.of(usuarioDe("juan.perez@sged.test")));

        UserDetails detalles = userDetailsService.loadUserByUsername("JUAN.PEREZ@SGED.TEST");

        assertThat(detalles.getUsername()).isEqualTo("juan.perez@sged.test");
    }

    @Test
    @DisplayName("el rol se expone con el prefijo ROLE_ que espera hasRole(...)")
    void elRolLlevaPrefijoRole() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue(anyString()))
                .thenReturn(Optional.of(usuarioDe("juan.perez@sged.test")));

        UserDetails detalles = userDetailsService.loadUserByUsername("juan.perez@sged.test");

        assertThat(detalles.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ESTUDIANTE");
    }

    @Test
    @DisplayName("los espacios sobrantes al pegar credenciales no impiden entrar")
    void usernameConEspaciosEncuentraLaCuenta() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("juan.perez@sged.test"))
                .thenReturn(Optional.of(usuarioDe("juan.perez@sged.test")));

        UserDetails detalles = userDetailsService.loadUserByUsername("  juan.perez@sged.test  ");

        assertThat(detalles.getUsername()).isEqualTo("juan.perez@sged.test");
    }

    @Test
    @DisplayName("un username inexistente sigue dando UsernameNotFoundException")
    void usernameInexistenteLanzaExcepcion() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nadie@sged.test"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
