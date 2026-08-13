package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.dto.RepresentantePageResponse;
import org.uteq.backend.academico.representante.dto.RepresentanteRequest;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.entity.RepresentanteEstudiante;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.academico.representante.dto.VinculoRequest;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.academico.representante.service.RepresentanteService;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepresentanteServiceTest {

    @Mock private RepresentanteRepository representanteRepository;
    @Mock private RepresentanteEstudianteRepository vinculoRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;

    @InjectMocks
    private RepresentanteService representanteService;

    private Persona persona() {
        return Persona.builder().idPersona(1L).nombre("Ana").apellido("Vera")
                .cedula("1234567890").correo("ana@sged.test").build();
    }

    private Usuario usuario() {
        return Usuario.builder().idUsuario(1L).username("ana.vera@sged.test")
                .roles(Set.of(Rol.builder().idRol(1L).nombre("REPRESENTANTE").build())).build();
    }

    private Representante representante() {
        return Representante.builder()
                .idRepresentante(1L)
                .persona(persona())
                .usuario(usuario())
                .parentesco("Madre")
                .activo(true)
                .build();
    }

    private Estudiante estudiante(long id, String nombre) {
        return Estudiante.builder()
                .idEstudiante(id)
                .persona(Persona.builder().nombre(nombre).apellido("Hijo").build())
                .categoria(Categoria.builder().idCategoria(1L).nombre("SUB-12").build())
                .build();
    }

    @Test
    @DisplayName("listar delega en el repositorio y mapea persona/usuario")
    void listar_devuelve_pagina_mapeada() {
        Page<Representante> pagina = new PageImpl<>(List.of(representante()), PageRequest.of(0, 10), 1);
        when(representanteRepository.findByActivoTrue(any())).thenReturn(pagina);
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of());

        RepresentantePageResponse<RepresentanteResponse> resultado = representanteService.listar(PageRequest.of(0, 10));

        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content().get(0).nombre()).isEqualTo("Ana");
        assertThat(resultado.content().get(0).username()).isEqualTo("ana.vera@sged.test");
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(representanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> representanteService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("crear rechaza cuando la persona ya es representante")
    void crear_persona_duplicada_lanza_excepcion() {
        RepresentanteRequest request = new RepresentanteRequest(1L, 2L, "Madre", "0999999999", null);
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(true);

        assertThatThrownBy(() -> representanteService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrada");

        verify(representanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza cuando el usuario ya esta asignado a otro representante")
    void crear_usuario_duplicado_lanza_excepcion() {
        RepresentanteRequest request = new RepresentanteRequest(1L, 2L, "Madre", "0999999999", null);
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(representanteRepository.existsByUsuario_IdUsuario(2L)).thenReturn(true);

        assertThatThrownBy(() -> representanteService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está asignado");
    }

    @Test
    @DisplayName("crear rechaza cuando el usuario no tiene el rol REPRESENTANTE")
    void crear_sin_rol_representante_lanza_excepcion() {
        RepresentanteRequest request = new RepresentanteRequest(1L, 1L, "Madre", "0999999999", null);
        Usuario usuarioSinRol = Usuario.builder().idUsuario(1L).username("ana.vera@sged.test")
                .roles(Set.of(Rol.builder().idRol(2L).nombre("ENTRENADOR").build())).build();
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(representanteRepository.existsByUsuario_IdUsuario(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioSinRol));

        assertThatThrownBy(() -> representanteService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rol REPRESENTANTE");

        verify(representanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear vincula de una vez los estudiantes iniciales pedidos")
    void crear_vincula_estudiantes_iniciales() {
        RepresentanteRequest request = new RepresentanteRequest(1L, 1L, "Madre", "0999999999", List.of(10L, 20L));
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(representanteRepository.existsByUsuario_IdUsuario(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(representanteRepository.save(any(Representante.class))).thenAnswer(inv -> {
            Representante r = inv.getArgument(0);
            r.setIdRepresentante(5L);
            return r;
        });
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, "Juan")));
        when(estudianteRepository.findById(20L)).thenReturn(Optional.of(estudiante(20L, "Maria")));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(vinculoRepository.save(any(RepresentanteEstudiante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(5L)).thenReturn(List.of());

        RepresentanteResponse resultado = representanteService.crear(request);

        assertThat(resultado.idRepresentante()).isEqualTo(5L);
        verify(vinculoRepository, times(2)).save(any(RepresentanteEstudiante.class));
    }

    @Test
    @DisplayName("crear lanza RecursoNoEncontradoException si un estudiante inicial no existe")
    void crear_falla_si_estudiante_inicial_no_existe() {
        RepresentanteRequest request = new RepresentanteRequest(1L, 1L, "Madre", null, List.of(999L));
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(representanteRepository.existsByUsuario_IdUsuario(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(representanteRepository.save(any(Representante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estudianteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> representanteService.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("eliminar hace baja logica del representante, no de sus vinculos")
    void eliminar_hace_baja_logica() {
        Representante existente = representante();
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(representanteRepository.save(any(Representante.class))).thenAnswer(inv -> inv.getArgument(0));

        representanteService.eliminar(1L);

        assertThat(existente.getActivo()).isFalse();
    }

    @Test
    @DisplayName("desvincularEstudiante desactiva el vinculo sin tocar la cuenta")
    void desvincular_desactiva_el_vinculo() {
        RepresentanteEstudiante vinculo = RepresentanteEstudiante.builder()
                .idRepresentanteEstudiante(7L)
                .representante(representante())
                .estudiante(estudiante(10L, "Juan"))
                .activo(true)
                .build();
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.of(vinculo));
        when(vinculoRepository.save(any(RepresentanteEstudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        representanteService.desvincularEstudiante(1L, 10L);

        assertThat(vinculo.getActivo()).isFalse();
    }

    @Test
    @DisplayName("desvincularEstudiante lanza RecursoNoEncontradoException si no habia vinculo")
    void desvincular_sin_vinculo_lanza_excepcion() {
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> representanteService.desvincularEstudiante(1L, 10L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("vincularEstudiante guarda la relacion y el contacto principal del vinculo")
    void vincular_guarda_relacion_y_contacto_principal() {
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante()));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, "Juan")));
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(10L)).thenReturn(List.of());
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.empty());
        when(vinculoRepository.save(any(RepresentanteEstudiante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of());

        representanteService.vincularEstudiante(1L, 10L, new VinculoRequest("Madre", true));

        ArgumentCaptor<RepresentanteEstudiante> captor = ArgumentCaptor.forClass(RepresentanteEstudiante.class);
        verify(vinculoRepository).save(captor.capture());
        assertThat(captor.getValue().getRelacion()).isEqualTo("Madre");
        assertThat(captor.getValue().getContactoPrincipal()).isTrue();
    }

    @Test
    @DisplayName("designar un contacto principal desmarca al anterior del mismo estudiante")
    void vincular_principal_desmarca_al_anterior() {
        RepresentanteEstudiante anterior = RepresentanteEstudiante.builder()
                .idRepresentanteEstudiante(7L)
                .representante(Representante.builder().idRepresentante(2L).build())
                .estudiante(estudiante(10L, "Juan"))
                .activo(true)
                .contactoPrincipal(true)
                .build();

        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante()));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, "Juan")));
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(10L)).thenReturn(List.of(anterior));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.empty());
        when(vinculoRepository.save(any(RepresentanteEstudiante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of());

        representanteService.vincularEstudiante(1L, 10L, new VinculoRequest("Padre", true));

        assertThat(anterior.getContactoPrincipal()).isFalse();
    }
}
