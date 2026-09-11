package org.uteq.backend.academico.student.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.dto.StudentPageResponse;
import org.uteq.backend.academico.student.dto.StudentRequest;
import org.uteq.backend.academico.student.dto.StudentResponse;
import org.uteq.backend.academico.student.dto.EnableAccessRequest;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianStudentRepository;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.config.RedisCacheConfig;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.time.LocalDate;
import java.time.Period;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio de {@code Student}: alta (con reactivación de una
 * ficha inactiva de la misma persona), edición por reasignación selectiva,
 * baja y reactivación lógicas, y las operaciones de conjunto por categoría
 * que delegan en procedimientos almacenados. El alta valida además que la
 * edad de la persona caiga en el rango de la categoría.
 *
 * <p>El cruce al dominio de seguridad (crear la cuenta del estudiante,
 * validar coherencia de rol) vive en {@link StudentAccessService};
 * extraerlo bajó el fan-out interno de esta clase, el más alto del sistema
 * (hallazgo MET-01 / R-06 del informe de evaluación de calidad).
 */
@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository estudianteRepository;
    private final PersonRepository personaRepository;
    private final CategoriaRepository categoriaRepository;
    private final GeneralStatusRepository estadoGeneralRepository;
    private final PosicionRepository posicionRepository;
    private final GuardianStudentRepository representanteEstudianteRepository;

    private final StudentAccessService estudianteAccesoService;

    /**
     * Lista paginada de estudiantes.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, envuelta en {@link StudentPageResponse}
     */
    @Cacheable(value = RedisCacheConfig.CACHE_STUDENTS, key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public StudentPageResponse<StudentResponse> list(Pageable pageable) {
        Page<Student> page = estudianteRepository.findAll(pageable);

        List<StudentResponse> content = page.getContent().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
        return new StudentPageResponse<>(
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
     * @throws ResourceNotFoundException si no existe o está inactivado
     */
    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        Student e = estudianteRepository.findByIdEstudianteAndActivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + id));
        return toResponse(e);
    }

    /**
     * Registra un estudiante sobre una persona ya existente. Si la persona
     * tuvo antes una ficha de estudiante y está inactiva, la reactiva y la
     * actualiza en lugar de crear una fila nueva.
     *
     * @param request datos del estudiante
     * @return el estudiante registrado o reactivado
     * @throws ResourceNotFoundException si la persona, la categoría o el
     *                                      estado referidos no existen
     * @throws IllegalArgumentException     si la persona ya tiene ficha
     *                                      activa, si el código de estudiante
     *                                      está en uso o si la edad no cae en
     *                                      el rango de la categoría
     */
    @Audited(action = "CREAR", entity = "Estudiante", idSpel = "#result.idEstudiante",
            descriptionSpel = "'creó la ficha de estudiante de ' + #result.nombrePersona + ' ' + #result.apellidoPersona")
    @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true)
    @Transactional
    public StudentResponse create(StudentRequest request) {
        estudianteAccesoService.validateConsistencyWithStudentRecord(request.idPersona());

        // 1. ¿La persona YA tiene un registro como estudiante (activo o inactivo)?
        Optional<Student> estudianteExistente = estudianteRepository.findByPersona_IdPersona(request.idPersona());

        if (estudianteExistente.isPresent()) {
            Student est = estudianteExistente.get();

            if (Boolean.TRUE.equals(est.getActive())) {
                throw new IllegalArgumentException("La persona seleccionada ya cuenta con una ficha de estudiante activa.");
            }

            // Estaba inactivo: se reactiva y se actualiza con los datos nuevos.
            Categoria categoria = categoriaRepository.findById(request.idCategoria())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + request.idCategoria()));

            GeneralStatus estadoGeneral = estadoGeneralRepository.findById(request.idEstadoGeneral())
                    .orElseThrow(() -> new ResourceNotFoundException("Estado General no encontrado: " + request.idEstadoGeneral()));

            est.setCategory(categoria);
            est.setGeneralStatus(estadoGeneral);
            est.setStudentCode(request.codigoEstudiante());
            est.setEnrollmentDate(request.fechaIngreso() != null ? request.fechaIngreso() : LocalDate.now(Zones.ECUADOR));
            est.setWeight(request.peso());
            est.setHeight(request.altura());
            est.setPosition(resolvePosition(request.idPosicion()));
            est.setActive(true);

            est = estudianteRepository.save(est);
            return toResponse(est);
        }

        // 2. La persona nunca fue estudiante: se crea un registro desde cero.
        if (estudianteRepository.existsByCodigoEstudiante(request.codigoEstudiante())) {
            throw new IllegalArgumentException("El código de estudiante '" + request.codigoEstudiante() + "' ya se encuentra en uso.");
        }

        Person persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con ID: " + request.idPersona()));

        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + request.idCategoria()));

        validateAgeInCategory(persona, categoria);

        GeneralStatus estadoGeneral = estadoGeneralRepository.findById(request.idEstadoGeneral())
                .orElseThrow(() -> new ResourceNotFoundException("Estado General no encontrado: " + request.idEstadoGeneral()));

        Student estudiante = Student.builder()
                .person(persona)
                .category(categoria)
                .generalStatus(estadoGeneral)
                .studentCode(request.codigoEstudiante())
                .enrollmentDate(request.fechaIngreso() != null ? request.fechaIngreso() : LocalDate.now(Zones.ECUADOR))
                .weight(request.peso())
                .height(request.altura())
                .position(resolvePosition(request.idPosicion()))
                .active(true)
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
     * @throws ResourceNotFoundException si el estudiante o alguna
     *                                      referencia nueva no existen
     * @throws IllegalArgumentException     si el código pertenece a otro
     *                                      estudiante, la persona nueva ya es
     *                                      estudiante o la edad no cae en el
     *                                      rango de la categoría nueva
     */
    @Audited(action = "EDITAR", entity = "Estudiante", idSpel = "#result.idEstudiante",
            descriptionSpel = "'editó la ficha de ' + #result.nombrePersona + ' ' + #result.apellidoPersona")
    @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true)
    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con id: " + id));

        // Si cambia de código, ese código no puede pertenecer a otro estudiante.
        if (estudianteRepository.existsByCodigoEstudianteAndIdEstudianteNot(request.codigoEstudiante(), id)) {
            throw new IllegalArgumentException("El código '" + request.codigoEstudiante() + "' ya está asignado a otro estudiante.");
        }

        reassignPersonIfChanged(estudiante, request.idPersona());
        reassignCategoryIfChanged(estudiante, request.idCategoria());
        reassignGeneralStatusIfChanged(estudiante, request.idEstadoGeneral());
        reassignPositionIfChanged(estudiante, request.idPosicion());

        estudiante.setStudentCode(request.codigoEstudiante());
        if (request.fechaIngreso() != null) {
            estudiante.setEnrollmentDate(request.fechaIngreso());
        }
        estudiante.setWeight(request.peso());
        estudiante.setHeight(request.altura());

        estudiante = estudianteRepository.save(estudiante);

        return toResponse(estudiante);
    }

    /**
     * Actualización estrecha de solo la posición nominal, para que
     * {@code ENTRENADOR} pueda asignarla, cambiarla o quitarla desde
     * evaluación diaria sin abrir la puerta a que edite categoría, código o
     * fecha de ingreso —eso sigue siendo de {@code ADMINISTRADOR} /
     * {@code RECEPCIONISTA} vía {@link #update}—.
     *
     * @param id         identificador del estudiante
     * @param idPosicion identificador de la posición, o {@code null} para
     *                   dejar al estudiante sin posición
     * @return el estudiante actualizado
     * @throws ResourceNotFoundException si el estudiante o la posición no
     *                                      existen
     */
    @Audited(action = "EDITAR", entity = "Estudiante", idSpel = "#result.idEstudiante",
            descriptionSpel = "'editó la posición de ' + #result.nombrePersona + ' ' + #result.apellidoPersona + ' a ' + (#result.nombrePosicion != null ? #result.nombrePosicion : 'sin posición')")
    @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true)
    @Transactional
    public StudentResponse updatePosition(Long id, Long idPosicion) {
        Student estudiante = estudianteRepository.findByIdEstudianteAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + id));
        estudiante.setPosition(resolvePosition(idPosicion));
        estudiante = estudianteRepository.save(estudiante);
        return toResponse(estudiante);
    }

    // R-09 (informe de evaluación de calidad): las reasignaciones de update()
    // seguían el mismo patrón —si el id pedido difiere del actual, buscar la
    // nueva fila y reasignarla— y sumaban complejidad al método. Extraídas
    // para que update() quede lineal: valida, reasigna lo que cambió, guarda.
    private void reassignPersonIfChanged(Student estudiante, Long idPersonaNueva) {
        if (estudiante.getPerson().getId().equals(idPersonaNueva)) {
            return;
        }
        if (estudianteRepository.existsByPersona_IdPersona(idPersonaNueva)) {
            throw new IllegalArgumentException("La nueva persona seleccionada ya es un estudiante registrado.");
        }
        Person nuevaPersona = personaRepository.findById(idPersonaNueva)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada con ID: " + idPersonaNueva));
        estudiante.setPerson(nuevaPersona);
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
    private void validateAgeInCategory(Person persona, Categoria categoria) {
        LocalDate nacimiento = persona.getBirthDate();
        if (nacimiento == null || categoria.getEdadMin() == null || categoria.getEdadMax() == null) {
            return;
        }

        int edad = Period.between(nacimiento, LocalDate.now(Zones.ECUADOR)).getYears();
        if (edad < categoria.getEdadMin() || edad > categoria.getEdadMax()) {
            throw new IllegalArgumentException(
                    persona.getName() + " " + persona.getLastName() + " tiene " + edad
                    + " años y " + categoria.getNombre() + " es para edades de "
                    + categoria.getEdadMin() + " a " + categoria.getEdadMax() + " años");
        }
    }

    private void reassignCategoryIfChanged(Student estudiante, Long idCategoriaNueva) {
        if (estudiante.getCategory().getIdCategoria().equals(idCategoriaNueva)) {
            return;
        }
        Categoria categoria = categoriaRepository.findById(idCategoriaNueva)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + idCategoriaNueva));
        validateAgeInCategory(estudiante.getPerson(), categoria);
        estudiante.setCategory(categoria);
    }

    private void reassignGeneralStatusIfChanged(Student estudiante, Long idEstadoGeneralNuevo) {
        if (estudiante.getGeneralStatus().getId().equals(idEstadoGeneralNuevo)) {
            return;
        }
        GeneralStatus estadoGeneral = estadoGeneralRepository.findById(idEstadoGeneralNuevo)
                .orElseThrow(() -> new ResourceNotFoundException("Estado General no encontrado: " + idEstadoGeneralNuevo));
        estudiante.setGeneralStatus(estadoGeneral);
    }

    // A diferencia de categoría/estadoGeneral, la posición es opcional y puede
    // pasar de asignada a sin asignar (idPosicionNueva null): hay que poder
    // desasignarla, no solo cambiarla.
    private void reassignPositionIfChanged(Student estudiante, Long idPosicionNueva) {
        Long actual = estudiante.getPosition() != null ? estudiante.getPosition().getIdPosicion() : null;
        if (java.util.Objects.equals(actual, idPosicionNueva)) {
            return;
        }
        estudiante.setPosition(resolvePosition(idPosicionNueva));
    }

    private Posicion resolvePosition(Long idPosicion) {
        if (idPosicion == null) {
            return null;
        }
        return posicionRepository.findById(idPosicion)
                .orElseThrow(() -> new ResourceNotFoundException("Posición no encontrada: " + idPosicion));
    }

    /**
     * Baja lógica de un estudiante ({@code activo = false}).
     *
     * @param id identificador del estudiante
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(action = "ELIMINAR", entity = "Estudiante", idSpel = "#p0",
            descriptionSpel = "'desactivó la ficha de estudiante #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Student estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con id: " + id));
        estudiante.setActive(false);
        estudianteRepository.save(estudiante);
    }

    /**
     * Reactiva la ficha de un estudiante dada de baja.
     *
     * @param id identificador del estudiante
     * @return el estudiante reactivado
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si la ficha ya está activa
     */
    @Audited(action = "REACTIVAR", entity = "Estudiante", idSpel = "#p0",
            descriptionSpel = "'reactivo la ficha de estudiante #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true)
    @Transactional
    public StudentResponse reactivate(Long id) {
        Student estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con id: " + id));

        if (Boolean.TRUE.equals(estudiante.getActive())) {
            throw new IllegalArgumentException("La ficha de estudiante ya se encuentra activa");
        }

        estudiante.setActive(true);
        return toResponse(estudianteRepository.save(estudiante));
    }

    /**
     * RF-50 / hallazgo H-03: anonimiza los datos del titular a solicitud del
     * representante legal. Delega en el procedimiento almacenado versionado
     * {@code academico.sp_anonimizar_estudiante} (migración {@code V27}), que
     * sustituye los datos identificativos de la persona por valores neutros,
     * borra el texto libre escrito sobre el menor y da de baja lógica la
     * ficha, conservando las claves foráneas y las estadísticas agregadas
     * (asistencia, evaluaciones, pagos). El acto queda en la bitácora de
     * auditoría ({@code @Audited}) e implementa el mecanismo de supresión que
     * RNF-22 exige.
     *
     * @param id identificador del estudiante a anonimizar
     * @throws ResourceNotFoundException si el estudiante no existe
     */
    @Audited(action = "ANONIMIZAR", entity = "Estudiante", idSpel = "#p0",
            descriptionSpel = "'anonimizó los datos personales del estudiante #' + #p0 + ' a solicitud del representante legal (RF-50 / derecho de supresión)'")
    @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true)
    @Transactional
    public void anonymize(Long id) {
        Student estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con id: " + id));
        estudianteRepository.anonymizeStudent(estudiante.getId());
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
    public long countActiveByCategory(Long idCategoria) {
        Long resultado = estudianteRepository.countActiveStudentsByCategory(idCategoria);
        return resultado != null ? resultado : 0L;
    }

    /**
     * Da de baja en bloque a todos los estudiantes activos de una categoría
     * (procedimiento almacenado).
     *
     * @param idCategoria identificador de la categoría
     */
    @Audited(action = "EDITAR", entity = "Estudiante",
            descriptionSpel = "'desactivó los estudiantes de la Categoria #' + #p0")
    @CacheEvict(value = RedisCacheConfig.CACHE_STUDENTS, allEntries = true)
    @Transactional
    public void deactivateByCategory(Long idCategoria) {
        estudianteRepository.deactivateStudentsByCategory(idCategoria);
    }

    /**
     * Sugiere el siguiente {@code codigo_estudiante} para un año; no reserva
     * nada, solo propone.
     *
     * @param anio año para el que se genera el código
     * @return el código propuesto
     */
    @Transactional(readOnly = true)
    public String generateNextCode(int anio) {
        return estudianteRepository.generateNextCode(anio);
    }

    /**
     * Devuelve {@code "Nombre Apellido - teléfono"} del representante activo
     * del estudiante, o {@code null} si no tiene.
     *
     * @param idEstudiante identificador del estudiante
     * @return el texto de contacto, o {@code null}
     * @throws ResourceNotFoundException si el estudiante no existe
     */
    @Transactional(readOnly = true)
    public String emergencyContact(Long idEstudiante) {
        if (!estudianteRepository.existsById(idEstudiante)) {
            throw new ResourceNotFoundException("Estudiante no encontrado con id: " + idEstudiante);
        }
        return representanteEstudianteRepository.contactoDe(idEstudiante);
    }

    /**
     * Habilita el acceso propio de un estudiante que ya existe: crea un
     * {@code UserAccount} (rol {@code ESTUDIANTE}) sobre la persona que el
     * estudiante ya tiene, sin duplicarla.
     *
     * @param idEstudiante identificador del estudiante
     * @param request      credenciales de la cuenta a crear
     * @return el estudiante con su acceso habilitado
     * @throws ResourceNotFoundException si el estudiante no existe
     * @throws IllegalArgumentException     si el estudiante ya tiene cuenta o
     *                                      el {@code username} está en uso
     */
    @Audited(action = "EDITAR", entity = "Estudiante", idSpel = "#result.idEstudiante",
            descriptionSpel = "'habilitó acceso al Student #' + #result.idEstudiante")
    @Transactional
    public StudentResponse enableAccess(Long idEstudiante, EnableAccessRequest request) {
        Student estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + idEstudiante));

        if (estudiante.getUserAccount() != null) {
            throw new IllegalArgumentException("Este estudiante ya tiene una cuenta de acceso");
        }

        UserAccount usuario = estudianteAccesoService.createStudentAccount(estudiante.getPerson(), request);

        estudiante.setUserAccount(usuario);
        estudiante = estudianteRepository.save(estudiante);
        return toResponse(estudiante);
    }

    // Mapeador privado entity -> DTO.
    private StudentResponse toResponse(Student e) {
        return new StudentResponse(
                e.getId(),
                e.getPerson() != null ? e.getPerson().getId() : null,
                e.getCategory() != null ? e.getCategory().getIdCategoria() : null,
                e.getGeneralStatus() != null ? e.getGeneralStatus().getId() : null,
                e.getPerson() != null ? e.getPerson().getName() : null,
                e.getPerson() != null ? e.getPerson().getLastName() : null,
                e.getCategory() != null ? e.getCategory().getNombre() : null,
                e.getGeneralStatus() != null ? e.getGeneralStatus().getName() : null,
                e.getStudentCode(),
                e.getEnrollmentDate(),
                e.getWeight(),
                e.getHeight(),
                e.getPosition() != null ? e.getPosition().getIdPosicion() : null,
                e.getPosition() != null ? e.getPosition().getNombre() : null,
                e.getPosition() != null ? e.getPosition().getAbreviatura() : null,
                e.getActive(),
                e.getCreatedAt()
        );
    }
}
