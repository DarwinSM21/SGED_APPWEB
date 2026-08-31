package org.uteq.backend.academico.estudiante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.academico.estudiante.dto.HabilitarAccesoRequest;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class EstudianteAccesoService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstadoGeneralRepository estadoGeneralRepository;
    private final PasswordEncoder passwordEncoder;

    public void validarCoherenciaConFichaEstudiante(Long idPersona) {
        usuarioRepository.findByPersona_IdPersonaAndActivoTrue(idPersona).ifPresent(usuario -> {
            boolean esEstudiante = usuario.getRoles() != null && usuario.getRoles().stream()
                    .anyMatch(r -> "ESTUDIANTE".equals(r.getNombre()));
            if (!esEstudiante) {
                throw new IllegalArgumentException(
                        "La persona tiene una cuenta con otro rol: no se le puede crear una ficha de estudiante");
            }
        });
    }

    public Usuario crearCuentaDeEstudiante(Persona persona, HabilitarAccesoRequest request) {
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese usuario");
        }

        Rol rolEstudiante = rolRepository.findByNombre("ESTUDIANTE")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ESTUDIANTE (ver db/seed.sql)"));
        EstadoGeneral estadoActivo = estadoGeneralRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el catalogo seguridad.estados_general (ver db/seed.sql)"));

        Usuario usuario = Usuario.builder()
                .persona(persona)
                .estadoGeneral(estadoActivo)
                .username(request.username())
                .password_Hash(passwordEncoder.encode(request.password()))
                .activo(true)
                .roles(Set.of(rolEstudiante))
                .build();
        return usuarioRepository.save(usuario);
    }
}
