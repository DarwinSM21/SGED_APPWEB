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
        // Se busca con el username saneado igual que se guarda (Usuario.
        // normalizarUsername): sin mayusculas y sin espacios sobrantes. Los
        // dos casos son reales al escribir en un celular -capitaliza la
        // primera letra- o al pegar credenciales -arrastra un espacio-, y
        // ambos terminaban en 401 sin ninguna pista de la causa.
        String buscado = username == null ? "" : username.trim();

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue(buscado)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + buscado));

        // 💡 Agregamos "ROLE_" para que reconozca los roles con hasRole(...)
        List<SimpleGrantedAuthority> autoridades = usuario.getRoles().stream()
                .map(r -> {
                    String nombreRol = r.getNombre().startsWith("ROLE_") 
                            ? r.getNombre() 
                            : "ROLE_" + r.getNombre();
                    return new SimpleGrantedAuthority(nombreRol);
                })
                .toList();
        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword_Hash())
                .authorities(autoridades)
                .accountLocked(!usuario.getActivo())
                .build();
    }
}