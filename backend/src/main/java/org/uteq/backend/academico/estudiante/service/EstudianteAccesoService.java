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

/**
 * Colaborador que concentra la relacion Estudiante-Usuario: la unica
 * porcion de EstudianteService que cruzaba de lleno al dominio de
 * seguridad (Usuario, Rol, PasswordEncoder). Extraido para bajar el
 * fan-out de EstudianteService (hallazgo MET-01 / R-06 del informe de
 * evaluacion de calidad: 18 dependencias internas, el mas alto del
 * sistema). No es un orquestador de alta completa -eso lo sigue llamando
 * EstudianteService, que es quien conoce cuando corresponde- sino el
 * lugar donde vive el conocimiento de como se arma y valida una cuenta
 * de rol ESTUDIANTE.
 */
@Service
@RequiredArgsConstructor
public class EstudianteAccesoService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstadoGeneralRepository estadoGeneralRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Guarda simetrica a UsuarioService.validarRolCoherente: si la persona
     * ya tiene cuenta, esa cuenta tiene que ser de estudiante. Sin cuenta
     * no hay nada que validar -- lo normal es que un estudiante no tenga
     * acceso al sistema, y si despues se le habilita, crearCuentaDeEstudiante
     * ya fija el rol ESTUDIANTE.
     */
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

    /**
     * Crea el Usuario (rol ESTUDIANTE) sobre una Persona que ya existe; no
     * lo asocia a la ficha de Estudiante, eso lo hace el llamador una vez
     * que tiene el Usuario guardado.
     */
    public Usuario crearCuentaDeEstudiante(Persona persona, HabilitarAccesoRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
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
