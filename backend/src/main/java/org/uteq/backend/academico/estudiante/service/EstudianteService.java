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

/**
 * Lógica de negocio de {@code Estudiante}: alta (con reactivación de una
 * ficha inactiva de la misma persona), edición por reasignación selectiva,
 * baja y reactivación lógicas, y las operaciones de conjunto por categoría
 * que delegan en procedimientos almacenados. El alta valida además que la
 * edad de la persona caiga en el rango de la categoría.
 *
 * <p>El cruce al dominio de seguridad (crear la cuenta del estudiante,
 * validar coherencia de rol) vive en {@link EstudianteAccesoService};
 * extraerlo bajó el fan-out interno de esta clase, el más alto del sistema
 * (hallazgo MET-01 / R-06 del informe de evaluación de calidad).
 */
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

    /**
     * Lista paginada de estudiantes.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, envuelta en {@link EstudiantePageResponse}
     */
    @Cacheable(value = RedisCacheConfig.CACHE_ESTUDIANTES, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public EstudiantePageResponse<EstudianteResponse> listar(Pageable pageable) {
        Page<Estudiante> page = estudianteRepository.findAll(pageable);

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

    /**
     * Busca un estudiante activo por su identificador.
     *
     * @param id identificador del estudiante
     * @return el estudiante encontrado
     * @throws RecursoNoEncontradoException si no existe o está inactivado
     */
    @Transactional(readOnly = true)
    public EstudianteResponse buscarPorId(Long id) {
        Estudiante e = estudianteRepository.findByIdEstudianteAndActivoTrue(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + id));
        return toResponse(e);
    }

    /**
     * Registra un estudiante sobre una persona ya existente. Si la persona
     * tuvo antes una ficha de estudiante y está inactiva, la reactiva y la
     * actualiza en lugar de crear una fila nueva.
     *
     * @param request datos del estudiante
     * @return el estudiante registrado o reactivado
     * @throws RecursoNoEncontradoException si la persona, la categoría o el
     *                                      estado referidos no existen
     * @throws IllegalArgumentException     si la persona ya tiene ficha
     *                                      activa, si el código de estudiante
     *                                      está en uso o si la edad no cae en
     *                                      el rango de la categoría
     */
    @Auditado(accion = "CREAR", entidad = "Estudiante", idSpel = "#result.idEstudiante",
            descripcionSpel = "'creó la ficha de estudiante de ' + #result.nombrePersona + ' ' + #result.apellidoPersona")
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        estudianteAccesoService.validarCoherenciaConFichaEstudiante(request.idPersona());

        // 1. ¿La persona YA tiene un registro como estudiante (activo o inactivo)?
        Optional<Estudiante> estudianteExistente = estudianteRepository.findByPersona_IdPersona(request.idPersona());

        if (estudianteExistente.isPresent()) {
            Estudiante est = estudianteExistente.get();

            if (Boolean.TRUE.equals(est.getActivo())) {
                throw new IllegalArgumentException("La persona seleccionada ya cuenta con una ficha de estudiante activa.");
            }

            // Estaba inactivo: se reactiva y se actualiza con los datos nuevos.
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

        // 2. La persona nunca fue estudiante: se crea un registro desde cero.
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

    /**
     * Actualiza la ficha de un estudiante reasignando solo lo que cambió
     * (persona, categoría, estado, posición) más los datos propios.
     *
     * @param id      identificador del estudiante a editar
     * @param request datos nuevos
     * @return el estudiante actualizado
     * @throws RecursoNoEncontradoException si el estudiante o alguna
     *                                      referencia nueva no existen
     * @throws IllegalArgumentException     si el código pertenece a otro
     *                                      estudiante, la persona nueva ya es
     *                                      estudiante o la edad no cae en el
     *                                      rango de la categoría nueva
     */
    @Auditado(accion = "EDITAR", entidad = "Estudiante", idSpel = "#result.idEstudiante",
            descripcionSpel = "'editó la ficha de ' + #result.nombrePersona + ' ' + #result.apellidoPersona")
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse editar(Long id, EstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));

        // Si cambia de código, ese código no puede pertenecer a otro estudiante.
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

    /**
     * Actualización estrecha de solo la posición nominal, para que
     * {@code ENTRENADOR} pueda asignarla, cambiarla o quitarla desde
     * evaluación diaria sin abrir la puerta a que edite categoría, código o
     * fecha de ingreso —eso sigue siendo de {@code ADMINISTRADOR} /
     * {@code RECEPCIONISTA} vía {@link #editar}—.
     *
     * @param id         identificador del estudiante
     * @param idPosicion identificador de la posición, o {@code null} para
     *                   dejar al estudiante sin posición
     * @return el estudiante actualizado
     * @throws RecursoNoEncontradoException si el estudiante o la posición no
     *                                      existen
     */
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

    // R-09 (informe de evaluación de calidad): las reasignaciones de editar()
    // seguían el mismo patrón —si el id pedido difiere del actual, buscar la
    // nueva fila y reasignarla— y sumaban complejidad al método. Extraídas
    // para que editar() quede lineal: valida, reasigna lo que cambió, guarda.
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

    /**
     * La edad del estudiante tiene que caer dentro del rango de su categoría.
     *
     * <p>Sin esta comprobación se podía matricular a alguien de 18 años en la
     * SUB-12 y el sistema respondía {@code 201} sin una advertencia. La
     * categoría decide en qué sesiones aparece para pasar lista, en qué
     * formación entra y en qué informe sale.
     *
     * <p>Se comprueba solo al asignar o cambiar la categoría, nunca en toda
     * edición: un estudiante que cumple años a mitad de temporada se sale del
     * rango sin que nadie haya hecho nada mal, y si la regla corriera siempre
     * quedaría imposible corregirle el peso o el teléfono. Sin fecha de
     * nacimiento no se valida nada.
     *
     * @param persona   persona cuya edad se evalúa
     * @param categoria categoría destino
     * @throws IllegalArgumentException si la edad queda fuera del rango
     *                                  {@code [edadMin, edadMax]}
     */
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

    // A diferencia de categoría/estadoGeneral, la posición es opcional y puede
    // pasar de asignada a sin asignar (idPosicionNueva null): hay que poder
    // desasignarla, no solo cambiarla.
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

    /**
     * Baja lógica de un estudiante ({@code activo = false}).
     *
     * @param id identificador del estudiante
     * @throws RecursoNoEncontradoException si no existe
     */
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

    /**
     * Reactiva la ficha de un estudiante dada de baja.
     *
     * @param id identificador del estudiante
     * @return el estudiante reactivado
     * @throws RecursoNoEncontradoException si no existe
     * @throws IllegalArgumentException     si la ficha ya está activa
     */
    @Auditado(accion = "REACTIVAR", entidad = "Estudiante", idSpel = "#p0",
            descripcionSpel = "'reactivo la ficha de estudiante #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public EstudianteResponse reactivar(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));

        if (Boolean.TRUE.equals(estudiante.getActivo())) {
            throw new IllegalArgumentException("La ficha de estudiante ya se encuentra activa");
        }

        estudiante.setActivo(true);
        return toResponse(estudianteRepository.save(estudiante));
    }

    /**
     * Cuenta los estudiantes activos de una categoría (procedimiento
     * almacenado).
     *
     * @param idCategoria identificador de la categoría
     * @return el número de estudiantes activos; {@code 0} si el procedimiento
     *         devuelve {@code null}
     */
    @Transactional(readOnly = true)
    public long contarActivosPorCategoria(Long idCategoria) {
        Long resultado = estudianteRepository.contarEstudiantesActivosPorCategoria(idCategoria);
        return resultado != null ? resultado : 0L;
    }

    /**
     * Da de baja en bloque a todos los estudiantes activos de una categoría
     * (procedimiento almacenado).
     *
     * @param idCategoria identificador de la categoría
     */
    @Auditado(accion = "EDITAR", entidad = "Estudiante",
            descripcionSpel = "'desactivó los estudiantes de la Categoria #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_ESTUDIANTES, allEntries = true)
    @Transactional
    public void desactivarPorCategoria(Long idCategoria) {
        estudianteRepository.desactivarEstudiantesPorCategoria(idCategoria);
    }

    /**
     * Sugiere el siguiente {@code codigo_estudiante} para un año; no reserva
     * nada, solo propone.
     *
     * @param anio año para el que se genera el código
     * @return el código propuesto
     */
    @Transactional(readOnly = true)
    public String generarSiguienteCodigo(int anio) {
        return estudianteRepository.generarSiguienteCodigo(anio);
    }

    /**
     * Devuelve {@code "Nombre Apellido - teléfono"} del representante activo
     * del estudiante, o {@code null} si no tiene.
     *
     * @param idEstudiante identificador del estudiante
     * @return el texto de contacto, o {@code null}
     * @throws RecursoNoEncontradoException si el estudiante no existe
     */
    @Transactional(readOnly = true)
    public String contactoDeEmergencia(Long idEstudiante) {
        if (!estudianteRepository.existsById(idEstudiante)) {
            throw new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante);
        }
        return representanteEstudianteRepository.contactoDe(idEstudiante);
    }

    /**
     * Habilita el acceso propio de un estudiante que ya existe: crea un
     * {@code Usuario} (rol {@code ESTUDIANTE}) sobre la persona que el
     * estudiante ya tiene, sin duplicarla.
     *
     * @param idEstudiante identificador del estudiante
     * @param request      credenciales de la cuenta a crear
     * @return el estudiante con su acceso habilitado
     * @throws RecursoNoEncontradoException si el estudiante no existe
     * @throws IllegalArgumentException     si el estudiante ya tiene cuenta o
     *                                      el {@code username} está en uso
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

    // Mapeador privado entidad -> DTO.
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
