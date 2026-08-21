package org.uteq.backend.inventario.asignacion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.inventario.articulo.repository.ArticuloRepository;
import org.uteq.backend.inventario.asignacion.dto.AsignacionDtos.*;
import org.uteq.backend.inventario.asignacion.entity.Asignacion;
import org.uteq.backend.inventario.asignacion.entity.Asignacion.EstadoAsignacion;
import org.uteq.backend.inventario.asignacion.entity.Asignacion.TipoDestinatario;
import org.uteq.backend.inventario.asignacion.repository.AsignacionRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.time.LocalDate;

/**
 * Entrega/devolucion de articulos a estudiantes o entrenadores. Crear
 * resta stock (mismo chequeo de no-negativo que un movimiento SALIDA);
 * devolver con estado DEVUELTO lo repone, PERDIDO no.
 */
@Service
@RequiredArgsConstructor
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final ArticuloRepository articuloRepository;
    private final EstudianteRepository estudianteRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Page<AsignacionResponse> listarPaginado(Pageable pageable) {
        return asignacionRepository.findAllByOrderByFechaAsignacionDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AsignacionResponse> listarPorEstudiante(Long idEstudiante, Pageable pageable) {
        return asignacionRepository.findByEstudiante_IdEstudianteOrderByFechaAsignacionDesc(idEstudiante, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AsignacionResponse> listarPorEntrenador(Long idEntrenador, Pageable pageable) {
        return asignacionRepository.findByEntrenador_IdEntrenadorOrderByFechaAsignacionDesc(idEntrenador, pageable)
                .map(this::toResponse);
    }

    @Auditado(accion = "CREAR", entidad = "Asignacion", idSpel = "#result.idAsignacion",
            descripcionSpel = "'asignó ' + #result.cantidad + ' de ' + #result.articulo + ' a ' + (#result.estudiante != null ? #result.estudiante : #result.entrenador)")
    @Transactional
    public AsignacionResponse crear(AsignacionRequest request, String usernameRegistrador) {
        validarDestinatario(request.tipoDestinatario(), request.idEstudiante(), request.idEntrenador());

        Articulo articulo = buscarArticulo(request.idArticulo());
        int nuevoStock = articulo.getStockActual() - request.cantidad();
        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente: hay " + articulo.getStockActual() + " unidades de \""
                            + articulo.getNombre() + "\" y se intentan asignar " + request.cantidad());
        }
        articulo.setStockActual(nuevoStock);
        articuloRepository.save(articulo);

        Usuario registrador = buscarUsuario(usernameRegistrador);

        Asignacion.AsignacionBuilder builder = Asignacion.builder()
                .articulo(articulo)
                .cantidad(request.cantidad())
                .tipoDestinatario(request.tipoDestinatario())
                .fechaAsignacion(LocalDate.now(Zonas.ECUADOR))
                .fechaDevolucionEsperada(request.fechaDevolucionEsperada())
                .estado(EstadoAsignacion.ASIGNADO)
                .registradoPor(registrador)
                .observaciones(request.observaciones());

        if (request.tipoDestinatario() == TipoDestinatario.ESTUDIANTE) {
            builder.estudiante(buscarEstudiante(request.idEstudiante()));
        } else {
            builder.entrenador(buscarEntrenador(request.idEntrenador()));
        }

        return toResponse(asignacionRepository.save(builder.build()));
    }

    @Auditado(accion = "EDITAR", entidad = "Asignacion", idSpel = "#result.idAsignacion",
            descripcionSpel = "'registró ' + #result.estado + ' de ' + #result.articulo + ' (asignación #' + #result.idAsignacion + ')'")
    @Transactional
    public AsignacionResponse devolver(Long id, DevolucionRequest request) {
        if (request.estado() == EstadoAsignacion.ASIGNADO) {
            throw new IllegalArgumentException("El estado de devolución debe ser DEVUELTO o PERDIDO");
        }

        Asignacion asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación no encontrada con ID: " + id));

        if (asignacion.getEstado() != EstadoAsignacion.ASIGNADO) {
            throw new IllegalArgumentException(
                    "La asignación #" + id + " ya fue resuelta como " + asignacion.getEstado());
        }

        if (request.estado() == EstadoAsignacion.DEVUELTO) {
            Articulo articulo = asignacion.getArticulo();
            articulo.setStockActual(articulo.getStockActual() + asignacion.getCantidad());
            articuloRepository.save(articulo);
        }

        asignacion.setEstado(request.estado());
        asignacion.setFechaDevolucionReal(LocalDate.now(Zonas.ECUADOR));
        if (request.observaciones() != null && !request.observaciones().isBlank()) {
            asignacion.setObservaciones(request.observaciones());
        }

        return toResponse(asignacionRepository.save(asignacion));
    }

    private void validarDestinatario(TipoDestinatario tipo, Long idEstudiante, Long idEntrenador) {
        boolean esEstudiante = tipo == TipoDestinatario.ESTUDIANTE;
        if (esEstudiante && (idEstudiante == null || idEntrenador != null)) {
            throw new IllegalArgumentException(
                    "Para tipoDestinatario ESTUDIANTE se requiere idEstudiante y no idEntrenador");
        }
        if (!esEstudiante && (idEntrenador == null || idEstudiante != null)) {
            throw new IllegalArgumentException(
                    "Para tipoDestinatario ENTRENADOR se requiere idEntrenador y no idEstudiante");
        }
    }

    private Articulo buscarArticulo(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Artículo no encontrado con ID: " + id));
    }

    private Estudiante buscarEstudiante(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con ID: " + id));
    }

    private Entrenador buscarEntrenador(Long id) {
        return entrenadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador no encontrado con ID: " + id));
    }

    private Usuario buscarUsuario(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + username));
    }

    private AsignacionResponse toResponse(Asignacion a) {
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

        return new AsignacionResponse(
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
