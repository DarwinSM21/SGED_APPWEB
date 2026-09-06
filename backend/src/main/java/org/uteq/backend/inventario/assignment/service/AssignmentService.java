package org.uteq.backend.inventario.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
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
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

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
    private final EstudianteRepository estudianteRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Lista paginada de asignaciones, de la más reciente a la más antigua.
     *
     * @param pageable paginación
     * @return la página, mapeada a {@link AssignmentResponse}
     */
    @Transactional(readOnly = true)
    public Page<AssignmentResponse> listPaged(Pageable pageable) {
        return asignacionRepository.findAllByOrderByFechaAsignacionDesc(pageable).map(this::toResponse);
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
        return asignacionRepository.findByEstudiante_IdEstudianteOrderByFechaAsignacionDesc(idEstudiante, pageable)
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
        return asignacionRepository.findByEntrenador_IdEntrenadorOrderByFechaAsignacionDesc(idEntrenador, pageable)
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
    @Auditado(accion = "CREAR", entidad = "Asignacion", idSpel = "#result.idAsignacion",
            descripcionSpel = "'asignó ' + #result.cantidad + ' de ' + #result.articulo + ' a ' + (#result.estudiante != null ? #result.estudiante : #result.entrenador)")
    @Transactional
    public AssignmentResponse create(AssignmentRequest request, String usernameRegistrador) {
        validateRecipient(request.tipoDestinatario(), request.idEstudiante(), request.idEntrenador());

        Item articulo = findItem(request.idArticulo());
        int nuevoStock = articulo.getStockActual() - request.cantidad();
        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente: hay " + articulo.getStockActual() + " unidades de \""
                            + articulo.getNombre() + "\" y se intentan asignar " + request.cantidad());
        }
        articulo.setStockActual(nuevoStock);
        articuloRepository.save(articulo);

        Usuario registrador = findUser(usernameRegistrador);

        Assignment.AssignmentBuilder builder = Assignment.builder()
                .articulo(articulo)
                .cantidad(request.cantidad())
                .tipoDestinatario(request.tipoDestinatario())
                .fechaAsignacion(LocalDate.now(Zones.ECUADOR))
                .fechaDevolucionEsperada(request.fechaDevolucionEsperada())
                .estado(AssignmentStatus.ASIGNADO)
                .registradoPor(registrador)
                .observaciones(request.observaciones());

        if (request.tipoDestinatario() == RecipientType.ESTUDIANTE) {
            builder.estudiante(findStudent(request.idEstudiante()));
        } else {
            builder.entrenador(findCoach(request.idEntrenador()));
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
    @Auditado(accion = "EDITAR", entidad = "Asignacion", idSpel = "#result.idAsignacion",
            descripcionSpel = "'registró ' + #result.estado + ' de ' + #result.articulo + ' (asignación #' + #result.idAsignacion + ')'")
    @Transactional
    public AssignmentResponse registerReturn(Long id, ReturnRequest request) {
        if (request.estado() == AssignmentStatus.ASIGNADO) {
            throw new IllegalArgumentException("El estado de devolución debe ser DEVUELTO o PERDIDO");
        }

        Assignment asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada con ID: " + id));

        if (asignacion.getEstado() != AssignmentStatus.ASIGNADO) {
            throw new IllegalArgumentException(
                    "La asignación #" + id + " ya fue resuelta como " + asignacion.getEstado());
        }

        if (request.estado() == AssignmentStatus.DEVUELTO) {
            Item articulo = asignacion.getArticulo();
            articulo.setStockActual(articulo.getStockActual() + asignacion.getCantidad());
            articuloRepository.save(articulo);
        }

        asignacion.setEstado(request.estado());
        asignacion.setFechaDevolucionReal(LocalDate.now(Zones.ECUADOR));
        if (request.observaciones() != null && !request.observaciones().isBlank()) {
            asignacion.setObservaciones(request.observaciones());
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

    private Estudiante findStudent(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con ID: " + id));
    }

    private Entrenador findCoach(Long id) {
        return entrenadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado con ID: " + id));
    }

    private Usuario findUser(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + username));
    }

    private AssignmentResponse toResponse(Assignment a) {
        var registrador = a.getRegistradoPor().getPersona();
        String nombreEstudiante = null;
        Long idEstudiante = null;
        if (a.getEstudiante() != null) {
            idEstudiante = a.getEstudiante().getIdEstudiante();
            var p = a.getEstudiante().getPersona();
            nombreEstudiante = p.getNombre() + " " + p.getApellido();
        }
        String nombreEntrenador = null;
        Long idEntrenador = null;
        if (a.getEntrenador() != null) {
            idEntrenador = a.getEntrenador().getIdEntrenador();
            var p = a.getEntrenador().getPersona();
            nombreEntrenador = p.getNombre() + " " + p.getApellido();
        }

        return new AssignmentResponse(
                a.getIdAsignacion(),
                a.getArticulo().getIdArticulo(),
                a.getArticulo().getNombre(),
                a.getCantidad(),
                a.getTipoDestinatario(),
                idEstudiante,
                nombreEstudiante,
                idEntrenador,
                nombreEntrenador,
                a.getFechaAsignacion(),
                a.getFechaDevolucionEsperada(),
                a.getFechaDevolucionReal(),
                a.getEstado(),
                registrador.getNombre() + " " + registrador.getApellido(),
                a.getObservaciones(),
                a.getCreatedAt()
        );
    }
}
