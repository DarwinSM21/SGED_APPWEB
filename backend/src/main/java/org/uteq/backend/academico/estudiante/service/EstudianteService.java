package org.uteq.backend.academico.estudiante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.dto.EstudiantePageResponse;
import org.uteq.backend.academico.estudiante.dto.EstudianteRequest;
import org.uteq.backend.academico.estudiante.dto.EstudianteResponse;
import org.uteq.backend.academico.estudiante.dto.HabilitarAccesoRequest;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final PersonaRepository personaRepository;
    private final CategoriaRepository categoriaRepository;
    private final EstadoGeneralRepository estadoGeneralRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final RepresentanteEstudianteRepository representanteEstudianteRepository;

    @Cacheable(value = RedisCacheConfig.CACHE_ESTUDIANTES, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public EstudiantePageResponse<EstudianteResponse> listar(Pageable pageable) {
        Page<Estudiante> page = estudianteRepository.findByActivoTrue(pageable);
        // var content = page.getContent().stream().map(this::toResponse).toList();
        List<EstudianteResponse> content = page.getContent().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
        return new EstudiantePageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public EstudianteResponse buscarPorId(Long id) {
        Estudiante e = estudianteRepository.findByIdEstudianteAndActivoTrue(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + id));
        return toResponse(e);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        // 1. Buscar si la persona YA tiene un registro como estudiante (activo o inactivo)
        Optional<Estudiante> estudianteExistente = estudianteRepository.findByPersona_IdPersona(request.idPersona());

        if (estudianteExistente.isPresent()) {
            Estudiante est = estudianteExistente.get();
            
            // Si ya está activo, lanzamos la excepción
            if (Boolean.TRUE.equals(est.getActivo())) {
                throw new IllegalArgumentException("La persona seleccionada ya cuenta con una ficha de estudiante activa.");
            }

            // Si estaba inactivo (activo = false), LO REACTIVAMOS Y ACTUALIZAMOS
            Categoria categoria = categoriaRepository.findById(request.idCategoria())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + request.idCategoria()));

            EstadoGeneral estadoGeneral = estadoGeneralRepository.findById(request.idEstadoGeneral())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Estado General no encontrado: " + request.idEstadoGeneral()));

            est.setCategoria(categoria);
            est.setEstadoGeneral(estadoGeneral);
            est.setCodigoEstudiante(request.codigoEstudiante());
            est.setFechaIngreso(request.fechaIngreso() != null ? request.fechaIngreso() : LocalDate.now());
            est.setPeso(request.peso());
            est.setAltura(request.altura());
            est.setActivo(true); // 👈 Re-activación del registro

            est = estudianteRepository.save(est);
            return toResponse(est);
        }

        // 2. Si la persona NUNCA ha sido estudiante, procede a crear un nuevo registro desde cero
        if (estudianteRepository.existsByCodigoEstudiante(request.codigoEstudiante())) {
            throw new IllegalArgumentException("El código de estudiante '" + request.codigoEstudiante() + "' ya se encuentra en uso.");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + request.idPersona()));

        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + request.idCategoria()));

        EstadoGeneral estadoGeneral = estadoGeneralRepository.findById(request.idEstadoGeneral())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado General no encontrado: " + request.idEstadoGeneral()));

        Estudiante estudiante = Estudiante.builder()
                .persona(persona)
                .categoria(categoria)
                .estadoGeneral(estadoGeneral)
                .codigoEstudiante(request.codigoEstudiante())
                .fechaIngreso(request.fechaIngreso() != null ? request.fechaIngreso() : LocalDate.now())
                .peso(request.peso())
                .altura(request.altura())
                .activo(true)
                .build();

        estudiante = estudianteRepository.save(estudiante);
        return toResponse(estudiante);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse editar(Long id, EstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));

        // VALIDACIÓN 3: Verificar que si cambia de código, este no le pertenezca a OTRO estudiante
        if (estudianteRepository.existsByCodigoEstudianteAndIdEstudianteNot(request.codigoEstudiante(), id)) {
            throw new IllegalArgumentException("El código '" + request.codigoEstudiante() + "' ya está asignado a otro estudiante.");
        }

        // Reasignar Persona si cambió (Y validar que la nueva persona tampoco tenga ya una ficha de estudiante)
        if (!estudiante.getPersona().getIdPersona().equals(request.idPersona())) {
            if (estudianteRepository.existsByPersona_IdPersona(request.idPersona())) {
                throw new IllegalArgumentException("La nueva persona seleccionada ya es un estudiante registrado.");
            }
            Persona nuevaPersona = personaRepository.findById(request.idPersona())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + request.idPersona()));
            estudiante.setPersona(nuevaPersona);
        }

        // Reasignar Categoría si cambió
        if (!estudiante.getCategoria().getIdCategoria().equals(request.idCategoria())) {
            Categoria categoria = categoriaRepository.findById(request.idCategoria())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + request.idCategoria()));
            estudiante.setCategoria(categoria);
        }

        // Reasignar Estado General si cambió
        if (!estudiante.getEstadoGeneral().getIdEstadoGeneral().equals(request.idEstadoGeneral())) {
            EstadoGeneral estadoGeneral = estadoGeneralRepository.findById(request.idEstadoGeneral())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Estado General no encontrado: " + request.idEstadoGeneral()));
            estudiante.setEstadoGeneral(estadoGeneral);
        }

        // Actualizar datos propios del estudiante
        estudiante.setCodigoEstudiante(request.codigoEstudiante());
        if (request.fechaIngreso() != null) {
            estudiante.setFechaIngreso(request.fechaIngreso());
        }
        estudiante.setPeso(request.peso());
        estudiante.setAltura(request.altura());

        estudiante = estudianteRepository.save(estudiante);

        return toResponse(estudiante);
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public void eliminar(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));
        estudiante.setActivo(false);
        estudianteRepository.save(estudiante);
    }

    @Transactional(readOnly = true)
    public long contarActivosPorCategoria(Long idCategoria) {
        Long resultado = estudianteRepository.contarEstudiantesActivosPorCategoria(idCategoria);
        return resultado != null ? resultado : 0L;
    }

    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public void desactivarPorCategoria(Long idCategoria) {
        estudianteRepository.desactivarEstudiantesPorCategoria(idCategoria);
    }

    /** Sugerencia de siguiente codigo_estudiante para un anio; no reserva nada, solo propone. */
    @Transactional(readOnly = true)
    public String generarSiguienteCodigo(int anio) {
        return estudianteRepository.generarSiguienteCodigo(anio);
    }

    /** "Nombre Apellido - telefono" del representante activo del estudiante, o null si no tiene. */
    @Transactional(readOnly = true)
    public String contactoDeEmergencia(Long idEstudiante) {
        if (!estudianteRepository.existsById(idEstudiante)) {
            throw new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante);
        }
        return representanteEstudianteRepository.contactoDe(idEstudiante);
    }

    /**
     * Habilita el acceso propio de un estudiante que ya existe en el
     * sistema: crea un Usuario nuevo (rol ESTUDIANTE) sobre la Persona que
     * el estudiante YA tiene, sin duplicarla. Es distinto del alta de
     * Representante, que si crea una Persona nueva porque el tutor no
     * estaba antes en el sistema.
     */
    @Transactional
    public EstudianteResponse habilitarAcceso(Long idEstudiante, HabilitarAccesoRequest request) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante));

        if (estudiante.getUsuario() != null) {
            throw new IllegalArgumentException("Este estudiante ya tiene una cuenta de acceso");
        }
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese usuario");
        }

        Rol rolEstudiante = rolRepository.findByNombre("ESTUDIANTE")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ESTUDIANTE (ver db/seed.sql)"));
        EstadoGeneral estadoActivo = estadoGeneralRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el catalogo seguridad.estados_general (ver db/seed.sql)"));

        Usuario usuario = Usuario.builder()
                .persona(estudiante.getPersona())
                .estadoGeneral(estadoActivo)
                .username(request.username())
                .password_Hash(passwordEncoder.encode(request.password()))
                .activo(true)
                .roles(Set.of(rolEstudiante))
                .build();
        usuario = usuarioRepository.save(usuario);

        estudiante.setUsuario(usuario);
        estudiante = estudianteRepository.save(estudiante);
        return toResponse(estudiante);
    }

    // Mapeador privado Entity -> DTO
    private EstudianteResponse toResponse(Estudiante e) {
        return new EstudianteResponse(
                e.getIdEstudiante(),
                e.getPersona() != null ? e.getPersona().getIdPersona() : null,
                e.getCategoria() != null ? e.getCategoria().getIdCategoria() : null,
                e.getEstadoGeneral() != null ? e.getEstadoGeneral().getIdEstadoGeneral() : null,
                e.getPersona() != null ? e.getPersona().getNombre() : null,
                e.getPersona() != null ? e.getPersona().getApellido() : null,
                e.getCategoria() != null ? e.getCategoria().getNombre() : null,
                e.getEstadoGeneral() != null ? e.getEstadoGeneral().getNombre() : null,
                e.getCodigoEstudiante(),
                e.getFechaIngreso(),
                e.getPeso(),
                e.getAltura(),
                e.getActivo(),
                e.getCreatedAt() // 👈 Pasa directo e.getCreatedAt()
        );
    }
}