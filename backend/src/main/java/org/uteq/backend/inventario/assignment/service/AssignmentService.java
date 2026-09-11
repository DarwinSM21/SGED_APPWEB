package org.uteq.backend.inventario.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.inventario.item.entity.Item;
import org.uteq.backend.inventario.item.repository.ItemRepository;
import org.uteq.backend.inventario.assignment.dto.AssignmentDtos.*;
import org.uteq.backend.inventario.assignment.entity.Assignment;
import org.uteq.backend.inventario.assignment.entity.Assignment.AssignmentStatus;
import org.uteq.backend.inventario.assignment.entity.Assignment.RecipientType;
import org.uteq.backend.inventario.assignment.repository.AssignmentRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.time.LocalDate;

/**
 * Entrega y devolución de artículos a estudiantes o entrenadores. Crear
 * resta stock (mismo chequeo de no-negativo que un movimiento de salida);
 * devolver con estado {@code DEVUELTO} lo repone, {@code PERDIDO} no.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {
    private final AssignmentRepository asignacionRepository;
    private final ItemRepository articuloRepository;
    private final StudentRepository estudianteRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final UserAccountRepository usuarioRepository;

    /**
     * Lista paginada de asignaciones, de la más reciente a la más antigua.
     *
     * @param pageable paginación
     * @return la página, mapeada a {@link AssignmentResponse}
     */
    @Transactional(readOnly = true)
    public Page<AssignmentResponse> listPaged(Pageable pageable) {
        return asignacionRepository.findAllByOrderByAssignmentDateDesc(pageable).map(this::toResponse);
    }

    /**
     * Asignaciones de un estudiante.
     *
     * @param idEstudiante identificador del estudiante
     * @param pageable     paginación
     * @return la página de asignaciones del estudiante
     */
    @Transactional(readOnly = true)
    public Page<AssignmentResponse> listByStudent(Long idEstudiante, Pageable pageable) {
        return asignacionRepository.findByStudent_IdOrderByAssignmentDateDesc(idEstudiante, pageable)
                .map(this::toResponse);
    }

    /**
     * Asignaciones de un entrenador.
     *
     * @param idEntrenador identificador del entrenador
     * @param pageable     paginación
     * @return la página de asignaciones del entrenador
     */
    @Transactional(readOnly = true)
    public Page<AssignmentResponse> listByCoach(Long idEntrenador, Pageable pageable) {
        return asignacionRepository.findByCoach_IdEntrenadorOrderByAssignmentDateDesc(idEntrenador, pageable)
                .map(this::toResponse);
    }

    /**
     * Registra una asignación y descuenta la cantidad del stock del artículo.
     *
     * @param request             artículo, cantidad, destinatario (exactamente
     *                            estudiante <em>o</em> entrenador) y fecha
     *                            esperada de devolución
     * @param usernameRegistrador usuario que registra la asignación
     * @return la asignación creada
     * @throws ResourceNotFoundException si el artículo o el destinatario
     *                                      no existen
     * @throws IllegalArgumentException     si el destinatario está mal
     *                                      especificado o no hay stock
     *                                      suficiente
     */
    @Audited(action = "CREAR", entity = "Asignacion", idSpel = "#result.idAsignacion",
            descriptionSpel = "'asignó ' + #result.cantidad + ' de ' + #result.articulo + ' a ' + (#result.estudiante != null ? #result.estudiante : #result.entrenador)")
    @Transactional
    public AssignmentResponse create(AssignmentRequest request, String usernameRegistrador) {
        validateRecipient(request.tipoDestinatario(), request.idEstudiante(), request.idEntrenador());

        Item articulo = findItem(request.idArticulo());
        int nuevoStock = articulo.getCurrentStock() - request.cantidad();
        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente: hay " + articulo.getCurrentStock() + " unidades de \""
                            + articulo.getName() + "\" y se intentan asignar " + request.cantidad());
        }
        articulo.setCurrentStock(nuevoStock);
        articuloRepository.save(articulo);

        UserAccount registrador = findUser(usernameRegistrador);

        Assignment.AssignmentBuilder builder = Assignment.builder()
                .item(articulo)
                .quantity(request.cantidad())
                .recipientType(request.tipoDestinatario())
                .assignmentDate(LocalDate.now(Zones.ECUADOR))
                .expectedReturnDate(request.fechaDevolucionEsperada())
                .status(AssignmentStatus.ASIGNADO)
                .registeredBy(registrador)
                .notes(request.observaciones());

        if (request.tipoDestinatario() == RecipientType.ESTUDIANTE) {
            builder.student(findStudent(request.idEstudiante()));
        } else {
            builder.coach(findCoach(request.idEntrenador()));
        }

        return toResponse(asignacionRepository.save(builder.build()));
    }

    /**
     * Resuelve una asignación como {@code DEVUELTO} (repone stock) o
     * {@code PERDIDO} (no repone).
     *
     * @param id      identificador de la asignación
     * @param request estado de la devolución y observaciones
     * @return la asignación actualizada
     * @throws ResourceNotFoundException si la asignación no existe
     * @throws IllegalArgumentException     si el estado es {@code ASIGNADO} o
     *                                      la asignación ya estaba resuelta
     */
    @Audited(action = "EDITAR", entity = "Asignacion", idSpel = "#result.idAsignacion",
            descriptionSpel = "'registró ' + #result.estado + ' de ' + #result.articulo + ' (asignación #' + #result.idAsignacion + ')'")
    @Transactional
    public AssignmentResponse registerReturn(Long id, ReturnRequest request) {
        if (request.estado() == AssignmentStatus.ASIGNADO) {
            throw new IllegalArgumentException("El estado de devolución debe ser DEVUELTO o PERDIDO");
        }

        Assignment asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada con ID: " + id));

        if (asignacion.getStatus() != AssignmentStatus.ASIGNADO) {
            throw new IllegalArgumentException(
                    "La asignación #" + id + " ya fue resuelta como " + asignacion.getStatus());
        }

        if (request.estado() == AssignmentStatus.DEVUELTO) {
            Item articulo = asignacion.getItem();
            articulo.setCurrentStock(articulo.getCurrentStock() + asignacion.getQuantity());
            articuloRepository.save(articulo);
        }

        asignacion.setStatus(request.estado());
        asignacion.setActualReturnDate(LocalDate.now(Zones.ECUADOR));
        if (request.observaciones() != null && !request.observaciones().isBlank()) {
            asignacion.setNotes(request.observaciones());
        }

        return toResponse(asignacionRepository.save(asignacion));
    }

    private void validateRecipient(RecipientType tipo, Long idEstudiante, Long idEntrenador) {
        boolean esEstudiante = tipo == RecipientType.ESTUDIANTE;
        if (esEstudiante && (idEstudiante == null || idEntrenador != null)) {
            throw new IllegalArgumentException(
                    "Para tipoDestinatario ESTUDIANTE se requiere idEstudiante y no idEntrenador");
        }
        if (!esEstudiante && (idEntrenador == null || idEstudiante != null)) {
            throw new IllegalArgumentException(
                    "Para tipoDestinatario ENTRENADOR se requiere idEntrenador y no idEstudiante");
        }
    }

    private Item findItem(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));
    }

    private Student findStudent(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con ID: " + id));
    }

    private Entrenador findCoach(Long id) {
        return entrenadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado con ID: " + id));
    }

    private UserAccount findUser(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + username));
    }

    private AssignmentResponse toResponse(Assignment a) {
        var registrador = a.getRegisteredBy().getPerson();
        String nombreEstudiante = null;
        Long idEstudiante = null;
        if (a.getStudent() != null) {
            idEstudiante = a.getStudent().getId();
            var p = a.getStudent().getPerson();
            nombreEstudiante = p.getName() + " " + p.getLastName();
        }
        String nombreEntrenador = null;
        Long idEntrenador = null;
        if (a.getCoach() != null) {
            idEntrenador = a.getCoach().getIdEntrenador();
            var p = a.getCoach().getPersona();
            nombreEntrenador = p.getName() + " " + p.getLastName();
        }

        return new AssignmentResponse(
                a.getId(),
                a.getItem().getId(),
                a.getItem().getName(),
                a.getQuantity(),
                a.getRecipientType(),
                idEstudiante,
                nombreEstudiante,
                idEntrenador,
                nombreEntrenador,
                a.getAssignmentDate(),
                a.getExpectedReturnDate(),
                a.getActualReturnDate(),
                a.getStatus(),
                registrador.getName() + " " + registrador.getLastName(),
                a.getNotes(),
                a.getCreatedAt()
        );
    }
}
