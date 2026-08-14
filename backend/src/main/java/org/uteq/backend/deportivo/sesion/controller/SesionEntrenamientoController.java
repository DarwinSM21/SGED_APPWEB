package org.uteq.backend.deportivo.sesion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionDiariaRepository;
import org.uteq.backend.deportivo.horario.service.HorarioService;
import org.uteq.backend.deportivo.sesion.dto.SesionCrearRequest;
import org.uteq.backend.deportivo.sesion.dto.SesionHoyResponse;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Punto de entrada del entrenador (y, desde RECEPCIONISTA, de la pantalla de
 * QR) a sus sesiones: cuales hay hoy, el historial completo, y el alta de
 * una nueva.
 *
 * <p>Sin /hoy, la pantalla de evaluacion diaria (que exige un id de sesion en
 * la ruta) no tiene forma de descubrirse a si misma: el entrenador tendria
 * que conocer el numero de antemano. Lo mismo le pasaria a recepcion para
 * elegir para cual sesion mostrar el QR.
 */
@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionEntrenamientoController {

    private final SesionEntrenamientoRepository sesionRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final EvaluacionDiariaRepository evaluacionRepository;
    private final CategoriaRepository categoriaRepository;
    private final HorarioService horarioService;

    /**
     * Sesiones de hoy. Un ADMINISTRADOR o RECEPCIONISTA ve todas (el
     * recepcionista necesita elegir cualquiera de ellas para mostrar su QR,
     * no esta atado a un entrenador concreto); un ENTRENADOR solo las suyas,
     * resuelto desde su propio usuario autenticado y no desde un parametro
     * que el cliente pudiera manipular para ver la agenda de otro.
     *
     * <p>Antes de leer, se generan las sesiones del dia que salen de un
     * horario fijo (ver HorarioService.generarSesionesDeHoy()) -por eso ya
     * no es de solo lectura-: asi ni el entrenador ni recepcion dependen de
     * que alguien haya creado la sesion a mano para que aparezca hoy.
     */
    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<List<SesionHoyResponse>> hoy() {
        horarioService.generarSesionesDeHoy();
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        boolean veTodasLasSesiones = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")
                        || a.getAuthority().equals("ROLE_RECEPCIONISTA"));

        List<SesionEntrenamiento> sesiones;
        if (veTodasLasSesiones) {
            sesiones = sesionRepository.findByFechaOrderByHoraInicioAsc(hoy);
        } else {
            var entrenador = entrenadorAutenticado();
            sesiones = entrenador == null
                    ? List.of()
                    : sesionRepository.findByFechaOrderByHoraInicioAsc(hoy).stream()
                        .filter(s -> s.getEntrenador().getIdEntrenador().equals(entrenador.getIdEntrenador()))
                        .toList();
        }

        return ResponseEntity.ok(sesiones.stream().map(this::aResponse).toList());
    }

    /**
     * Historial completo del entrenador autenticado (pasadas y futuras), no
     * solo las de hoy: sin esto, cualquier dia sin sesion programada dejaba
     * al entrenador sin forma de llegar a una evaluacion o plantilla pasada
     * desde la interfaz.
     */
    @GetMapping("/mias")
    @PreAuthorize("hasRole('ENTRENADOR')")
    @Transactional
    public ResponseEntity<List<SesionHoyResponse>> mias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        horarioService.generarSesionesDeHoy();
        Entrenador entrenador = entrenadorAutenticado();
        if (entrenador == null) {
            return ResponseEntity.ok(List.of());
        }

        Page<SesionEntrenamiento> pagina = sesionRepository.findByEntrenadorIdEntrenadorOrderByFechaDesc(
                entrenador.getIdEntrenador(), PageRequest.of(page, size));
        return ResponseEntity.ok(pagina.map(this::aResponse).getContent());
    }

    /**
     * Alta de una sesion propia. El idEntrenador nunca viene del cliente: se
     * resuelve del usuario autenticado, igual que en /hoy y /mias, para que
     * un entrenador no pueda crear una sesion "a nombre" de otro con solo
     * cambiar un id en el body.
     */
    @PostMapping
    @PreAuthorize("hasRole('ENTRENADOR')")
    @Transactional
    public ResponseEntity<SesionHoyResponse> crear(@Valid @RequestBody SesionCrearRequest request) {
        Entrenador entrenador = entrenadorAutenticado();
        if (entrenador == null) {
            throw new RecursoNoEncontradoException("No hay un entrenador asociado a esta cuenta");
        }

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: " + request.idCategoria()));

        SesionEntrenamiento sesion = SesionEntrenamiento.builder()
                .entrenador(entrenador)
                .categoria(categoria)
                .fecha(request.fecha())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .campo(request.campo())
                .estado("PROGRAMADA")
                .build();

        sesion = sesionRepository.save(sesion);
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(sesion));
    }

    private Entrenador entrenadorAutenticado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return entrenadorRepository.findByUsuario_Username(username).orElse(null);
    }

    private SesionHoyResponse aResponse(SesionEntrenamiento s) {
        var persona = s.getEntrenador().getPersona();
        return new SesionHoyResponse(
                s.getIdSesion(),
                s.getCategoria().getNombre(),
                persona.getNombre() + " " + persona.getApellido(),
                s.getFecha(),
                s.getHoraInicio(),
                s.getHoraFin(),
                s.getCampo(),
                s.getEstado(),
                evaluacionRepository.existsBySesionIdSesion(s.getIdSesion()));
    }
}
