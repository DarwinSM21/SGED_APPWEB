package org.uteq.backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.entity.UsuarioRol;
import java.util.List;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
    List<UsuarioRol> findByUsuario(Usuario usuario);
}