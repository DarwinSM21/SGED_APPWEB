package org.uteq.backend.seguridad.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserAccountRepository usuarioRepository;

    /**
     * Resuelve una cuenta activa por nombre de usuario (sin distinguir
     * mayúsculas/minúsculas) y la adapta al {@link UserDetails} que Spring
     * Security usa para autenticar, con un {@code ROLE_} por cada rol de la
     * cuenta.
     *
     * @param username nombre de usuario a autenticar
     * @return los datos de autenticación de esa cuenta
     * @throws UsernameNotFoundException si no existe una cuenta activa con ese nombre de usuario
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String buscado = username == null ? "" : username.trim();

        UserAccount usuario = usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue(buscado)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + buscado));

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
