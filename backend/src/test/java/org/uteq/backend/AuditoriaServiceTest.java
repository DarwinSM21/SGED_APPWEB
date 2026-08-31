package org.uteq.backend;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.uteq.backend.seguridad.auditoria.entity.Auditoria;
import org.uteq.backend.seguridad.auditoria.repository.AuditoriaRepository;
import org.uteq.backend.seguridad.auditoria.service.AuditoriaService;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
        RequestContextHolder.resetRequestAttributes();
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

    @Test
    @DisplayName("registrar sin autenticacion en el contexto usa 'desconocido' y rol nulo")
    void registrarSinAutenticacionUsaDesconocido() {
        when(usuarioRepository.findByUsername("desconocido")).thenReturn(Optional.empty());

        servicio.registrar("EDITAR", "Lesion", 45L, "editó Lesion #45");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertEquals("desconocido", captor.getValue().getUsuarioNombre());
        assertNull(captor.getValue().getRol());
    }

    @Test
    @DisplayName("registrarConIdentidad vincula la fila de Usuario cuando existe")
    void registrarConIdentidadVinculaUsuarioExistente() {
        Usuario usuario = Usuario.builder().idUsuario(7L).username("ana.torres").build();
        when(usuarioRepository.findByUsername("ana.torres")).thenReturn(Optional.of(usuario));

        servicio.registrarConIdentidad("ana.torres", "ADMINISTRADOR", "LOGIN", "Usuario", null, "inició sesión");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertEquals(usuario, captor.getValue().getUsuario());
    }

    @Test
    @DisplayName("registrarConIdentidad dentro de una peticion HTTP real resuelve la IP remota")
    void registrarConIdentidadResuelveIpDesdeElRequestActual() {
        when(usuarioRepository.findByUsername("ana.torres")).thenReturn(Optional.empty());
        var requestHttp = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(requestHttp.getRemoteAddr()).thenReturn("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestHttp));

        servicio.registrarConIdentidad("ana.torres", "ADMINISTRADOR", "LOGIN", "Usuario", null, "inició sesión");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertEquals("10.0.0.5", captor.getValue().getIp());
    }

    @Test
    @DisplayName("buscar con todos los filtros presentes arma un predicado sin lanzar")
    void buscarConTodosLosFiltrosArmaElPredicado() {
        var pageable = PageRequest.of(0, 20);
        when(auditoriaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        servicio.buscar("ana", "EDITAR", "Lesion",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), pageable);

        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(auditoriaRepository).findAll(captor.capture(), eq(pageable));
        ejecutarPredicado(captor.getValue());
    }

    @Test
    @DisplayName("buscar sin ningun filtro tambien arma un predicado valido")
    void buscarSinFiltrosArmaElPredicado() {
        var pageable = PageRequest.of(0, 20);
        when(auditoriaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        servicio.buscar(null, null, null, null, null, pageable);

        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(auditoriaRepository).findAll(captor.capture(), eq(pageable));
        ejecutarPredicado(captor.getValue());
    }

    @Test
    @DisplayName("buscar con filtros de texto en blanco los trata como ausentes")
    void buscarConFiltrosEnBlancoLosIgnora() {
        var pageable = PageRequest.of(0, 20);
        when(auditoriaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        servicio.buscar("   ", "", null, null, null, pageable);

        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(auditoriaRepository).findAll(captor.capture(), eq(pageable));
        ejecutarPredicado(captor.getValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void ejecutarPredicado(Specification filtro) {
        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Expression lowerExpr = mock(Expression.class);
        Predicate predicado = mock(Predicate.class);

        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(cb.lower(any(Expression.class))).thenReturn(lowerExpr);
        lenient().when(cb.like(any(Expression.class), anyString())).thenReturn(predicado);
        lenient().when(cb.equal(any(), any())).thenReturn(predicado);
        lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(predicado);
        lenient().when(cb.lessThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(predicado);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicado);

        assertDoesNotThrow(() -> filtro.toPredicate(root, query, cb));
    }
}
