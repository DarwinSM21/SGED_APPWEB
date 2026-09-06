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
import org.springframework.data.domain.Pageable;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.dto.GuardianPageResponse;
import org.uteq.backend.academico.guardian.dto.GuardianRequest;
import org.uteq.backend.academico.guardian.dto.GuardianResponse;
import org.uteq.backend.academico.guardian.entity.Guardian;
import org.uteq.backend.academico.guardian.entity.GuardianStudent;
import org.uteq.backend.academico.guardian.repository.GuardianStudentRepository;
import org.uteq.backend.academico.guardian.dto.LinkRequest;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.academico.guardian.service.GuardianService;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
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
class GuardianServiceTest {

    @Mock private GuardianRepository representanteRepository;
    @Mock private GuardianStudentRepository vinculoRepository;
    @Mock private PersonRepository personaRepository;
    @Mock private UserAccountRepository usuarioRepository;
    @Mock private StudentRepository estudianteRepository;

    @InjectMocks
    private GuardianService representanteService;

    private Person persona() {
        return Person.builder().idPersona(1L).nombre("Ana").apellido("Vera")
                .cedula("1234567890").correo("ana@sged.test").build();
    }

    private UserAccount usuario() {
        return UserAccount.builder().idUsuario(1L).username("ana.vera@sged.test")
                .roles(Set.of(Role.builder().idRol(1L).nombre("REPRESENTANTE").build())).build();
    }

    private Guardian representante() {
        return Guardian.builder()
                .idRepresentante(1L)
                .persona(persona())
                .usuario(usuario())
                .parentesco("Madre")
                .activo(true)
                .build();
    }

    private Student estudiante(long id, String nombre) {
        return Student.builder()
                .idEstudiante(id)
                .persona(Person.builder().nombre(nombre).apellido("Hijo").build())
                .categoria(Categoria.builder().idCategoria(1L).nombre("SUB-12").build())
                .build();
    }

    @Test
    @DisplayName("listar delega en el repositorio y mapea persona/usuario")
    void listar_devuelve_pagina_mapeada() {
        Page<Guardian> pagina = new PageImpl<>(List.of(representante()), PageRequest.of(0, 10), 1);
        when(representanteRepository.findAll(any(Pageable.class))).thenReturn(pagina);
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of());

        GuardianPageResponse<GuardianResponse> resultado = representanteService.list(PageRequest.of(0, 10));

        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content().get(0).nombre()).isEqualTo("Ana");
        assertThat(resultado.content().get(0).username()).isEqualTo("ana.vera@sged.test");
    }

    @Test
    @DisplayName("buscarPorId lanza ResourceNotFoundException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(representanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> representanteService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crear rechaza cuando la persona ya es representante")
    void crear_persona_duplicada_lanza_excepcion() {
        GuardianRequest request = new GuardianRequest(1L, 2L, "Madre", "0999999999", null);
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(true);

        assertThatThrownBy(() -> representanteService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrada");

        verify(representanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza cuando el usuario ya esta asignado a otro representante")
    void crear_usuario_duplicado_lanza_excepcion() {
        GuardianRequest request = new GuardianRequest(1L, 2L, "Madre", "0999999999", null);
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(representanteRepository.existsByUsuario_IdUsuario(2L)).thenReturn(true);

        assertThatThrownBy(() -> representanteService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está asignado");
    }

    @Test
    @DisplayName("crear rechaza cuando el usuario no tiene el rol REPRESENTANTE")
    void crear_sin_rol_representante_lanza_excepcion() {
        GuardianRequest request = new GuardianRequest(1L, 1L, "Madre", "0999999999", null);
        UserAccount usuarioSinRol = UserAccount.builder().idUsuario(1L).username("ana.vera@sged.test")
                .roles(Set.of(Role.builder().idRol(2L).nombre("ENTRENADOR").build())).build();
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(representanteRepository.existsByUsuario_IdUsuario(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioSinRol));

        assertThatThrownBy(() -> representanteService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rol REPRESENTANTE");

        verify(representanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear vincula de una vez los estudiantes iniciales pedidos")
    void crear_vincula_estudiantes_iniciales() {
        GuardianRequest request = new GuardianRequest(1L, 1L, "Madre", "0999999999", List.of(10L, 20L));
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(representanteRepository.existsByUsuario_IdUsuario(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(representanteRepository.save(any(Guardian.class))).thenAnswer(inv -> {
            Guardian r = inv.getArgument(0);
            r.setIdRepresentante(5L);
            return r;
        });
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, "Juan")));
        when(estudianteRepository.findById(20L)).thenReturn(Optional.of(estudiante(20L, "Maria")));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(vinculoRepository.save(any(GuardianStudent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(5L)).thenReturn(List.of());

        GuardianResponse resultado = representanteService.create(request);

        assertThat(resultado.idRepresentante()).isEqualTo(5L);
        verify(vinculoRepository, times(2)).save(any(GuardianStudent.class));
    }

    @Test
    @DisplayName("crear lanza ResourceNotFoundException si un estudiante inicial no existe")
    void crear_falla_si_estudiante_inicial_no_existe() {
        GuardianRequest request = new GuardianRequest(1L, 1L, "Madre", null, List.of(999L));
        when(representanteRepository.existsByPersona_IdPersona(1L)).thenReturn(false);
        when(representanteRepository.existsByUsuario_IdUsuario(1L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(representanteRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estudianteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> representanteService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("eliminar hace baja logica del representante, no de sus vinculos")
    void eliminar_hace_baja_logica() {
        Guardian existente = representante();
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(representanteRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));

        representanteService.delete(1L);

        assertThat(existente.getActivo()).isFalse();
    }

    @Test
    @DisplayName("desvincularEstudiante desactiva el vinculo sin tocar la cuenta")
    void desvincular_desactiva_el_vinculo() {
        GuardianStudent vinculo = GuardianStudent.builder()
                .idRepresentanteEstudiante(7L)
                .representante(representante())
                .estudiante(estudiante(10L, "Juan"))
                .activo(true)
                .build();
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.of(vinculo));
        when(vinculoRepository.save(any(GuardianStudent.class))).thenAnswer(inv -> inv.getArgument(0));

        representanteService.unlinkStudent(1L, 10L);

        assertThat(vinculo.getActivo()).isFalse();
    }

    @Test
    @DisplayName("desvincularEstudiante lanza ResourceNotFoundException si no habia vinculo")
    void desvincular_sin_vinculo_lanza_excepcion() {
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> representanteService.unlinkStudent(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("vincularEstudiante guarda la relacion y el contacto principal del vinculo")
    void vincular_guarda_relacion_y_contacto_principal() {
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante()));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, "Juan")));
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(10L)).thenReturn(List.of());
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.empty());
        when(vinculoRepository.save(any(GuardianStudent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of());

        representanteService.linkStudent(1L, 10L, new LinkRequest("Madre", true));

        ArgumentCaptor<GuardianStudent> captor = ArgumentCaptor.forClass(GuardianStudent.class);
        verify(vinculoRepository).save(captor.capture());
        assertThat(captor.getValue().getRelacion()).isEqualTo("Madre");
        assertThat(captor.getValue().getContactoPrincipal()).isTrue();
    }

    @Test
    @DisplayName("designar un contacto principal desmarca al anterior del mismo estudiante")
    void vincular_principal_desmarca_al_anterior() {
        GuardianStudent anterior = GuardianStudent.builder()
                .idRepresentanteEstudiante(7L)
                .representante(Guardian.builder().idRepresentante(2L).build())
                .estudiante(estudiante(10L, "Juan"))
                .activo(true)
                .contactoPrincipal(true)
                .build();

        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante()));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, "Juan")));
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(10L)).thenReturn(List.of(anterior));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.empty());
        when(vinculoRepository.save(any(GuardianStudent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of());

        representanteService.linkStudent(1L, 10L, new LinkRequest("Padre", true));

        assertThat(anterior.getContactoPrincipal()).isFalse();
    }

    @Test
    @DisplayName("editar actualiza parentesco y telefono sin tocar la persona ni el usuario vinculados")
    void editar_actualiza_parentesco_y_telefono() {
        Guardian existente = representante();
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(representanteRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of());

        GuardianRequest request = new GuardianRequest(1L, 1L, "Padre", "0999999999", null);
        GuardianResponse resultado = representanteService.update(1L, request);

        assertThat(resultado.parentesco()).isEqualTo("Padre");
        assertThat(resultado.telefonoContacto()).isEqualTo("0999999999");
    }

    @Test
    @DisplayName("editar lanza 404 cuando el representante no existe")
    void editar_inexistente_lanza_excepcion() {
        when(representanteRepository.findById(99L)).thenReturn(Optional.empty());

        GuardianRequest request = new GuardianRequest(1L, 1L, "Padre", "0999999999", null);

        assertThatThrownBy(() -> representanteService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("reactivar vuelve a activar una ficha de representante dada de baja")
    void reactivar_reactiva_una_ficha_inactiva() {
        Guardian existente = representante();
        existente.setActivo(false);
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(representanteRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of());

        GuardianResponse resultado = representanteService.reactivate(1L);

        assertThat(resultado.activo()).isTrue();
    }

    @Test
    @DisplayName("reactivar rechaza una ficha de representante que ya esta activa")
    void reactivar_rechaza_una_ficha_ya_activa() {
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante())); // activo = true

        assertThatThrownBy(() -> representanteService.reactivate(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra activa");
    }
}
