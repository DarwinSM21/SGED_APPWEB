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
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.LocalDate;
import java.time.Period;
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
    private final PosicionRepository posicionRepository;
    private final RepresentanteEstudianteRepository representanteEstudianteRepository;

    private final EstudianteAccesoService estudianteAccesoService;

    @Cacheable(value = RedisCacheConfig.CACHE_ESTUDIANTES, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public EstudiantePageResponse<EstudianteResponse> listar(Pageable pageable) {
        Page<Estudiante> page = estudianteRepository.findByActivoTrue(pageable);

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

    @Auditado(accion = "CREAR", entidad = "Estudiante", idSpel = "#result.idEstudiante",
            descripcionSpel = "'creó la ficha de estudiante de ' + #result.nombrePersona + ' ' + #result.apellidoPersona")
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        estudianteAccesoService.validarCoherenciaConFichaEstudiante(request.idPersona());

        Optional<Estudiante> estudianteExistente = estudianteRepository.findByPersona_IdPersona(request.idPersona());

        if (estudianteExistente.isPresent()) {
            Estudiante est = estudianteExistente.get();

            if (Boolean.TRUE.equals(est.getActivo())) {
                throw new IllegalArgumentException("La persona seleccionada ya cuenta con una ficha de estudiante activa.");
            }

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
            est.setPosicion(resolverPosicion(request.idPosicion()));
            est.setActivo(true);

            est = estudianteRepository.save(est);
            return toResponse(est);
        }

        if (estudianteRepository.existsByCodigoEstudiante(request.codigoEstudiante())) {
            throw new IllegalArgumentException("El código de estudiante '" + request.codigoEstudiante() + "' ya se encuentra en uso.");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + request.idPersona()));

        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + request.idCategoria()));

        validarEdadEnCategoria(persona, categoria);

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
                .posicion(resolverPosicion(request.idPosicion()))
                .activo(true)
                .build();

        estudiante = estudianteRepository.save(estudiante);
        return toResponse(estudiante);
    }

    @Auditado(accion = "EDITAR", entidad = "Estudiante", idSpel = "#result.idEstudiante",
            descripcionSpel = "'editó la ficha de ' + #result.nombrePersona + ' ' + #result.apellidoPersona")
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse editar(Long id, EstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));

        if (estudianteRepository.existsByCodigoEstudianteAndIdEstudianteNot(request.codigoEstudiante(), id)) {
            throw new IllegalArgumentException("El código '" + request.codigoEstudiante() + "' ya está asignado a otro estudiante.");
        }

        reasignarPersonaSiCambio(estudiante, request.idPersona());
        reasignarCategoriaSiCambio(estudiante, request.idCategoria());
        reasignarEstadoGeneralSiCambio(estudiante, request.idEstadoGeneral());
        reasignarPosicionSiCambio(estudiante, request.idPosicion());

        estudiante.setCodigoEstudiante(request.codigoEstudiante());
        if (request.fechaIngreso() != null) {
            estudiante.setFechaIngreso(request.fechaIngreso());
        }
        estudiante.setPeso(request.peso());
        estudiante.setAltura(request.altura());

        estudiante = estudianteRepository.save(estudiante);

        return toResponse(estudiante);
    }

    @Auditado(accion = "EDITAR", entidad = "Estudiante", idSpel = "#result.idEstudiante",
            descripcionSpel = "'editó la posición de ' + #result.nombrePersona + ' ' + #result.apellidoPersona + ' a ' + (#result.nombrePosicion != null ? #result.nombrePosicion : 'sin posición')")
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse actualizarPosicion(Long id, Long idPosicion) {
        Estudiante estudiante = estudianteRepository.findByIdEstudianteAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + id));
        estudiante.setPosicion(resolverPosicion(idPosicion));
        estudiante = estudianteRepository.save(estudiante);
        return toResponse(estudiante);
    }

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

    private void validarEdadEnCategoria(Persona persona, Categoria categoria) {
        LocalDate nacimiento = persona.getFechaNacimiento();
        if (nacimiento == null || categoria.getEdadMin() == null || categoria.getEdadMax() == null) {
            return;
        }

        int edad = Period.between(nacimiento, LocalDate.now(Zonas.ECUADOR)).getYears();
        if (edad < categoria.getEdadMin() || edad > categoria.getEdadMax()) {
            throw new IllegalArgumentException(
                    persona.getNombre() + " " + persona.getApellido() + " tiene " + edad
                    + " años y " + categoria.getNombre() + " es para edades de "
                    + categoria.getEdadMin() + " a " + categoria.getEdadMax() + " años");
        }
    }

    private void reasignarCategoriaSiCambio(Estudiante estudiante, Long idCategoriaNueva) {
        if (estudiante.getCategoria().getIdCategoria().equals(idCategoriaNueva)) {
            return;
        }
        Categoria categoria = categoriaRepository.findById(idCategoriaNueva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + idCategoriaNueva));
        validarEdadEnCategoria(estudiante.getPersona(), categoria);
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

    private void reasignarPosicionSiCambio(Estudiante estudiante, Long idPosicionNueva) {
        Long actual = estudiante.getPosicion() != null ? estudiante.getPosicion().getIdPosicion() : null;
        if (java.util.Objects.equals(actual, idPosicionNueva)) {
            return;
        }
        estudiante.setPosicion(resolverPosicion(idPosicionNueva));
    }

    private Posicion resolverPosicion(Long idPosicion) {
        if (idPosicion == null) {
            return null;
        }
        return posicionRepository.findById(idPosicion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Posición no encontrada: " + idPosicion));
    }

    @Auditado(accion = "ELIMINAR", entidad = "Estudiante", idSpel = "#p0",
            descripcionSpel = "'desactivó la ficha de estudiante #' + #p0")
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

    @Transactional(readOnly = true)
    public String generarSiguienteCodigo(int anio) {
        return estudianteRepository.generarSiguienteCodigo(anio);
    }

    @Transactional(readOnly = true)
    public String contactoDeEmergencia(Long idEstudiante) {
        if (!estudianteRepository.existsById(idEstudiante)) {
            throw new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante);
        }
        return representanteEstudianteRepository.contactoDe(idEstudiante);
    }

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
                e.getPosicion() != null ? e.getPosicion().getIdPosicion() : null,
                e.getPosicion() != null ? e.getPosicion().getNombre() : null,
                e.getPosicion() != null ? e.getPosicion().getAbreviatura() : null,
                e.getActivo(),
                e.getCreatedAt()
        );
    }
}
