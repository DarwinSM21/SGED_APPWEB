package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.reportes.service.ReportePdfService;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.usuario.controller.PerfilController;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PerfilControllerTest {

    @Mock private UsuarioRepository usuarioRepository;

    private PerfilController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // ReportePdfService no tiene dependencias: se usa la instancia real
        // para que la prueba tambien confirme que el PDF generado es valido.
        controller = new PerfilController(usuarioRepository, new ReportePdfService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

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
    @DisplayName("GET /api/usuarios/me/datos-pdf devuelve el PDF del usuario autenticado, sin restringir por rol")
    void descargaMisDatosParaCualquierRolAutenticado() throws Exception {
        autenticarComo("estudiante1", "ESTUDIANTE");
        Usuario usuario = Usuario.builder()
                .username("estudiante1")
                .persona(Persona.builder()
                        .nombre("Ana").apellido("Torres").cedula("1234567890")
                        .correo("ana@sged.test").telefono("0999999999")
                        .fechaNacimiento(LocalDate.of(2010, 5, 20))
                        .build())
                .roles(Set.of(Rol.builder().nombre("ESTUDIANTE").build()))
                .build();
        when(usuarioRepository.findByUsername("estudiante1")).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/api/usuarios/me/datos-pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("mis-datos.pdf")));
    }

    @Test
    @DisplayName("Usuario autenticado sin fila en la base da 404")
    void usuarioSinFilaDa404() throws Exception {
        autenticarComo("fantasma", "ESTUDIANTE");
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/usuarios/me/datos-pdf"))
                .andExpect(status().isNotFound());
    }
}
