package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uteq.backend.academico.estudiante.dto.HabilitarAccesoRequest;
import org.uteq.backend.academico.estudiante.service.EstudianteAccesoService;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MET-01 / R-06 (informe de evaluacion de calidad): logica extraida de
 * EstudianteService para bajar su fan-out. Antes se probaba indirectamente
 * a traves de EstudianteServiceTest; ahora vive y se prueba aqui, donde
 * esta el conocimiento real de como se arma una cuenta ESTUDIANTE.
 */
@ExtendWith(MockitoExtension.class)
class EstudianteAccesoServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private EstadoGeneralRepository estadoGeneralRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private EstudianteAccesoService service;

    private final Persona persona = Persona.builder().idPersona(1L).nombre("Ana").apellido("Vera").build();

    @Test
    @DisplayName("validarCoherenciaConFichaEstudiante no lanza si la persona no tiene cuenta")
    void validarCoherencia_sin_cuenta_no_lanza() {
        when(usuarioRepository.findByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(Optional.empty());

        service.validarCoherenciaConFichaEstudiante(1L);
    }

    @Test
    @DisplayName("validarCoherenciaConFichaEstudiante lanza si la cuenta existente es de otro rol")
    void validarCoherencia_con_cuenta_de_otro_rol_lanza() {
        Usuario cuentaEntrenador = Usuario.builder().idUsuario(9L)
                .roles(Set.of(Rol.builder().idRol(2L).nombre("ENTRENADOR").build())).build();
        when(usuarioRepository.findByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(Optional.of(cuentaEntrenador));

        assertThatThrownBy(() -> service.validarCoherenciaConFichaEstudiante(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validarCoherenciaConFichaEstudiante no lanza si la cuenta ya es de rol ESTUDIANTE")
    void validarCoherencia_con_cuenta_de_estudiante_no_lanza() {
        Usuario cuentaEstudiante = Usuario.builder().idUsuario(9L)
                .roles(Set.of(Rol.builder().idRol(5L).nombre("ESTUDIANTE").build())).build();
        when(usuarioRepository.findByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(Optional.of(cuentaEstudiante));

        service.validarCoherenciaConFichaEstudiante(1L);
    }

    @Test
    @DisplayName("crearCuentaDeEstudiante rechaza un username ya en uso, sin guardar nada")
    void crearCuenta_username_duplicado_lanza() {
        when(usuarioRepository.existsByUsername("dup@sged.test")).thenReturn(true);

        assertThatThrownBy(() -> service.crearCuentaDeEstudiante(
                persona, new HabilitarAccesoRequest("dup@sged.test", "password123")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearCuentaDeEstudiante crea el usuario con rol ESTUDIANTE, la contrasena hasheada y sobre la Persona dada")
    void crearCuenta_exitosa() {
        HabilitarAccesoRequest request = new HabilitarAccesoRequest("andres@sged.test", "password123");
        Rol rolEstudiante = Rol.builder().idRol(6L).nombre("ESTUDIANTE").build();

        when(usuarioRepository.existsByUsername("andres@sged.test")).thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(rolEstudiante));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(EstadoGeneral.builder().idEstadoGeneral(1L).build()));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setIdUsuario(9L);
            return u;
        });

        Usuario resultado = service.crearCuentaDeEstudiante(persona, request);

        assertThat(resultado.getIdUsuario()).isEqualTo(9L);
        assertThat(resultado.getPersona()).isSameAs(persona);
        assertThat(resultado.getPassword_Hash()).isEqualTo("$2a$12$encoded");
        assertThat(resultado.getRoles()).containsExactly(rolEstudiante);
    }

    @Test
    @DisplayName("crearCuentaDeEstudiante lanza IllegalStateException si falta el rol ESTUDIANTE en el catalogo")
    void crearCuenta_sin_rol_estudiante_en_catalogo_lanza() {
        when(usuarioRepository.existsByUsername("x@sged.test")).thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearCuentaDeEstudiante(persona, new HabilitarAccesoRequest("x@sged.test", "password123")))
                .isInstanceOf(IllegalStateException.class);
    }
}
