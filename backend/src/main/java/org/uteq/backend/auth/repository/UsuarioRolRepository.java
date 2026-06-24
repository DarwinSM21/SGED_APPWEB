package org.uteq.backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.auth.entity.UsuarioRol;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
}