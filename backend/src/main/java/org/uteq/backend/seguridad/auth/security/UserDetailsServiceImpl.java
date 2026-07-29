package org.uteq.backend.seguridad.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.List;

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

<<<<<<< HEAD:backend/src/main/java/org/uteq/backend/seguridad/auth/security/UserDetailsServiceImpl.java
        // 💡 Agregamos "ROLE_" para que reconozca los roles con hasRole(...)
        List<SimpleGrantedAuthority> autoridades = usuario.getRoles().stream()
                .map(r -> {
                    String nombreRol = r.getNombre().startsWith("ROLE_") 
                            ? r.getNombre() 
                            : "ROLE_" + r.getNombre();
                    return new SimpleGrantedAuthority(nombreRol);
                })
                .toList();
=======
        String rol = usuario.getRoles().stream()
                .findFirst()
                .map(r -> "ROLE_" + r.getNombre())
                .orElse("ROLE_USER");

        List<SimpleGrantedAuthority> autoridades = List.of(
                new SimpleGrantedAuthority(rol));
>>>>>>> origin/main:backend/src/main/java/org/uteq/backend/auth/security/UserDetailsServiceImpl.java

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword_Hash())
                .authorities(autoridades)
                .accountLocked(!usuario.getActivo())
                .build();
    }
}