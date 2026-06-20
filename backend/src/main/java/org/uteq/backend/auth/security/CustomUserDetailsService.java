package org.uteq.backend.auth.security;

import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado"
                        ));

        String rol = usuario.getUsuarioRoles()
                .stream()
                .findFirst()
                .orElseThrow()
                .getRol()
                .getNombre();

        return new User(
                usuario.getUsername(),
                usuario.getPasswordHash(),
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + rol
                        )
                )
        );
    }
}