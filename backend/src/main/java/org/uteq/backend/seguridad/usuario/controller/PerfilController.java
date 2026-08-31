package org.uteq.backend.seguridad.usuario.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.reportes.service.ReportePdfService;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios/me")
@RequiredArgsConstructor
public class PerfilController {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UsuarioRepository usuarioRepository;
    private final ReportePdfService pdfService;

    @GetMapping("/datos-pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarMisDatos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + auth.getName()));

        Persona persona = usuario.getPersona();
        String roles = usuario.getRoles() == null || usuario.getRoles().isEmpty()
                ? "-"
                : usuario.getRoles().stream().map(Rol::getNombre).reduce((a, b) -> a + ", " + b).orElse("-");

        List<List<String>> filas = List.of(
                List.of("Usuario", usuario.getUsername()),
                List.of("Rol", roles),
                List.of("Nombre", persona.getNombre() + " " + persona.getApellido()),
                List.of("Cédula", persona.getCedula()),
                List.of("Correo", persona.getCorreo()),
                List.of("Teléfono", persona.getTelefono() != null ? persona.getTelefono() : "-"),
                List.of("Fecha de nacimiento", persona.getFechaNacimiento().format(FECHA)),
                List.of("Cuenta creada el", usuario.getCreatedAt() != null ? usuario.getCreatedAt().format(FECHA) : "-")
        );

        byte[] pdf = pdfService.generar("Mis Datos", List.of("Campo", "Valor"), filas);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("mis-datos.pdf").build().toString())
                .body(pdf);
    }
}
