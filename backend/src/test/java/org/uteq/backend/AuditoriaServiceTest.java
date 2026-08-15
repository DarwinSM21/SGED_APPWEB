package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.uteq.backend.seguridad.auditoria.entity.Auditoria;
import org.uteq.backend.seguridad.auditoria.repository.AuditoriaRepository;
import org.uteq.backend.seguridad.auditoria.service.AuditoriaService;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock private AuditoriaRepository auditoriaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private AuditoriaService servicio;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username, String rol) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("registrar toma el usuario y rol del SecurityContext")
    void registrarUsaSecurityContext() {
        autenticarComo("ana.torres", "ADMINISTRADOR");
        when(usuarioRepository.findByUsername("ana.torres")).thenReturn(Optional.empty());

        servicio.registrar("EDITAR", "Lesion", 45L, "editó Lesion #45");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria guardada = captor.getValue();
        assertEquals("ana.torres", guardada.getUsuarioNombre());
        assertEquals("ADMINISTRADOR", guardada.getRol());
        assertEquals("EDITAR", guardada.getAccion());
        assertEquals("Lesion", guardada.getEntidad());
        assertEquals(45L, guardada.getEntidadId());
    }

    @Test
    @DisplayName("registrarConIdentidad no depende del SecurityContext (login/logout)")
    void registrarConIdentidadNoRequiereContexto() {
        when(usuarioRepository.findByUsername("ana.torres")).thenReturn(Optional.empty());

        servicio.registrarConIdentidad("ana.torres", "ADMINISTRADOR", "LOGIN", "Usuario", null, "inició sesión");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertEquals("ana.torres", captor.getValue().getUsuarioNombre());
        assertEquals("LOGIN", captor.getValue().getAccion());
    }

    @Test
    @DisplayName("un fallo al guardar no propaga excepcion: nunca debe romper la operacion auditada")
    void fallosAlGuardarNoPropagan() {
        autenticarComo("ana.torres", "ADMINISTRADOR");
        when(usuarioRepository.findByUsername("ana.torres")).thenReturn(Optional.empty());
        when(auditoriaRepository.save(any())).thenThrow(new RuntimeException("DB caida"));

        servicio.registrar("EDITAR", "Lesion", 45L, "editó Lesion #45");
        // No debe lanzar: si llega aqui, la operacion de negocio no se vio afectada.
    }

    @Test
    @DisplayName("buscar delega los filtros y el paginado al repositorio")
    void buscarDelegaAlRepositorio() {
        var pageable = PageRequest.of(0, 20);
        var fila = Auditoria.builder()
                .idAuditoria(1L).usuarioNombre("ana.torres").rol("ADMINISTRADOR")
                .accion("EDITAR").entidad("Lesion").entidadId(45L)
                .descripcion("editó Lesion #45").build();
        when(auditoriaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(fila)));

        var resultado = servicio.buscar("ana", "EDITAR", "Lesion", null, null, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("ana.torres", resultado.getContent().get(0).usuario());
        verify(auditoriaRepository, never()).save(any());
    }
}
