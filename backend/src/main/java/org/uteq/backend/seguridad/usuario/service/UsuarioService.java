package org.uteq.backend.seguridad.usuario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.dto.UsuarioPageResponse;
import org.uteq.backend.seguridad.usuario.dto.UsuarioRequest;
import org.uteq.backend.seguridad.usuario.dto.UsuarioResponse;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final EstadoGeneralRepository estadoGeneralRepository;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = RedisCacheConfig.CACHE_USUARIOS, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public UsuarioPageResponse<UsuarioResponse> listar(Pageable pageable) {
        Page<Usuario> page = usuarioRepository.findByActivoTrue(pageable);
        var content = page.getContent().stream().map(this::toResponse).toList();
        return new UsuarioPageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario u = usuarioRepository.findByIdUsuarioAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
        return toResponse(u);
 }

    @CacheEvict(value = RedisCacheConfig.CACHE_USUARIOS, allEntries = true)
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya se encuentra registrado");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con id: " + request.idPersona()));

        EstadoGeneral estado = estadoGeneralRepository.findById(request.idEstadoGeneral())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado general no encontrado con id: " + request.idEstadoGeneral()));

        Usuario usuario = Usuario.builder()
                .persona(persona)
                .estadoGeneral(estado)
                .username(request.username())
                .password_Hash(passwordEncoder.encode(request.password()))
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_USUARIOS, allEntries = true)
    @Transactional
    public UsuarioResponse editar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));

        if (!usuario.getUsername().equals(request.username()) && usuarioRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya está ocupado");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con id: " + request.idPersona()));

        EstadoGeneral estado = estadoGeneralRepository.findById(request.idEstadoGeneral())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado general no encontrado con id: " + request.idEstadoGeneral()));

        usuario.setPersona(persona);
        usuario.setEstadoGeneral(estado);
        usuario.setUsername(request.username());
        // Se codifica directamente ya que la validación @NotBlank en el DTO asegura que no viene vacía
        usuario.setPassword_Hash(passwordEncoder.encode(request.password()));

        usuario = usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_USUARIOS, allEntries = true)
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getIdUsuario(),
                u.getPersona().getIdPersona(),
                u.getPersona().getNombre(),
                u.getPersona().getApellido(),
                u.getPersona().getCorreo(),
                u.getEstadoGeneral().getIdEstadoGeneral(),
                u.getEstadoGeneral().getNombre(),
                u.getUsername(),
                u.getUltimoAcceso(),
                u.getActivo(),
                u.getCreatedAt()
        );
    }
}