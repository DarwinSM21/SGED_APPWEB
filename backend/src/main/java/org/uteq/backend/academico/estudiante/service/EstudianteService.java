package org.uteq.backend.academico.estudiante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.dto.EstudiantePageResponse;
import org.uteq.backend.academico.estudiante.dto.EstudianteRequest;
import org.uteq.backend.academico.estudiante.dto.EstudianteResponse;
import org.uteq.backend.academico.estudiante.dto.HabilitarAccesoRequest;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.LocalDate;
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
    private final RepresentanteEstudianteRepository representanteEstudianteRepository;
    // MET-01 / R-06 (informe de evaluacion de calidad): antes EstudianteService
    // inyectaba UsuarioRepository, RolRepository y PasswordEncoder directo
    // (fan-out interno de 18, el mas alto del sistema) para la porcion de
    // alta que cruza a seguridad -crear la cuenta y validar coherencia de
    // rol-. Esa porcion vive ahora en EstudianteAccesoService.
    private final EstudianteAccesoService estudianteAccesoService;

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

    @Auditado(accion = "CREAR", entidad = "Estudiante", idSpel = "#result.idEstudiante")
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        estudianteAccesoService.validarCoherenciaConFichaEstudiante(request.idPersona());

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
            est.setFechaIngreso(request.fechaIngreso() != null ? request.fechaIngreso() : LocalDate.now(Zonas.ECUADOR));
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
                .fechaIngreso(request.fechaIngreso() != null ? request.fechaIngreso() : LocalDate.now(Zonas.ECUADOR))
                .peso(request.peso())
                .altura(request.altura())
                .activo(true)
                .build();

        estudiante = estudianteRepository.save(estudiante);
        return toResponse(estudiante);
    }

    @Auditado(accion = "EDITAR", entidad = "Estudiante", idSpel = "#result.idEstudiante")
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

        reasignarPersonaSiCambio(estudiante, request.idPersona());
        reasignarCategoriaSiCambio(estudiante, request.idCategoria());
        reasignarEstadoGeneralSiCambio(estudiante, request.idEstadoGeneral());

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

    // R-09 (informe de evaluacion de calidad): las tres reasignaciones de
    // editar() seguian el mismo patron -si el id pedido difiere del actual,
    // buscar la nueva fila y reasignarla- y sumaban su propia complejidad al
    // metodo. Extraidas para que editar() quede lineal: valida, reasigna lo
    // que cambio, guarda.
    private void reasignarPersonaSiCambio(Estudiante estudiante, Long idPersonaNueva) {
        if (estudiante.getPersona().getIdPersona().equals(idPersonaNueva)) {
            return;
        }
        if (estudianteRepository.existsByPersona_IdPersona(idPersonaNueva)) {
            throw new IllegalArgumentException("La nueva persona seleccionada ya es un estudiante registrado.");
        }
        Persona nuevaPersona = personaRepository.findById(idPersonaNueva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + idPersonaNueva));
        estudiante.setPersona(nuevaPersona);
    }

    private void reasignarCategoriaSiCambio(Estudiante estudiante, Long idCategoriaNueva) {
        if (estudiante.getCategoria().getIdCategoria().equals(idCategoriaNueva)) {
            return;
        }
        Categoria categoria = categoriaRepository.findById(idCategoriaNueva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + idCategoriaNueva));
        estudiante.setCategoria(categoria);
    }

    private void reasignarEstadoGeneralSiCambio(Estudiante estudiante, Long idEstadoGeneralNuevo) {
        if (estudiante.getEstadoGeneral().getIdEstadoGeneral().equals(idEstadoGeneralNuevo)) {
            return;
        }
        EstadoGeneral estadoGeneral = estadoGeneralRepository.findById(idEstadoGeneralNuevo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado General no encontrado: " + idEstadoGeneralNuevo));
        estudiante.setEstadoGeneral(estadoGeneral);
    }

    @Auditado(accion = "ELIMINAR", entidad = "Estudiante", idSpel = "#p0")
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

    @Auditado(accion = "EDITAR", entidad = "Estudiante",
            descripcionSpel = "'desactivó los estudiantes de la Categoria #' + #p0")
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
    @Auditado(accion = "EDITAR", entidad = "Estudiante", idSpel = "#result.idEstudiante",
            descripcionSpel = "'habilitó acceso al Estudiante #' + #result.idEstudiante")
    @Transactional
    public EstudianteResponse habilitarAcceso(Long idEstudiante, HabilitarAccesoRequest request) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante));

        if (estudiante.getUsuario() != null) {
            throw new IllegalArgumentException("Este estudiante ya tiene una cuenta de acceso");
        }

        Usuario usuario = estudianteAccesoService.crearCuentaDeEstudiante(estudiante.getPersona(), request);

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