package org.uteq.backend.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.repository.UsuarioRepository;

import java.util.List;

/**
 * Implementacion de UserDetailsService que consulta la base de datos
 * a traves de UsuarioRepository. Spring Security lo usa en la cadena
 * de autenticacion para cargar el usuario durante el login.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        String rol = usuario.getRoles().stream()
                .findFirst()
                .map(r -> r.getNombre())
                .orElse("ROLE_USER");

        List<SimpleGrantedAuthority> autoridades = List.of(
                new SimpleGrantedAuthority(rol));

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(autoridades)
                .accountLocked(!usuario.getActivo())
                .build();
    }
}
