package org.uteq.backend.auth.service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.auth.dto.LoginRequest;
import org.uteq.backend.auth.dto.LoginResponse;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.repository.UsuarioRepository;
import org.uteq.backend.auth.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        Usuario usuario = usuarioRepository
                .findByUsername(request.username()) 
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));


        if (!usuario.getActivo()) {
                throw new RuntimeException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(
                request.password(),
                usuario.getPasswordHash())) {

                throw new RuntimeException("Credenciales incorrectas");
        }

        String rol = usuario.getUsuarioRoles()
                .stream()
                .findFirst()
                .orElseThrow()
                .getRol()
                .getNombre();

        String token = jwtService.generateToken(
                usuario.getUsername(),
                rol
        );

        return LoginResponse.builder()
                .token(token)
                .username(usuario.getUsername())
                .nombre(
                        usuario.getPersona().getNombre()
                                + " "
                                + usuario.getPersona().getApellido()
                )
                .rol(rol)
                .build();
    }
}
