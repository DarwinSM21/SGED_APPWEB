package org.uteq.backend.deportivo.sesion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Logica de negocio de sesiones de entrenamiento, antes embebida en
 * SesionEntrenamientoController (hallazgo D-03 del informe de evaluacion de
 * calidad: 167 lineas de controlador con 5 repositorios inyectados). El
 * controlador conserva la resolucion de identidad desde SecurityContextHolder
 * (mismo patron que InformeService: el principal se resuelve en el
 * controller y este servicio se prueba con un String cualquiera, sin
 * simular contexto de seguridad) y delega aqui el resto.
 */
@Service
@RequiredArgsConstructor
public class SesionEntrenamientoService {

    private final SesionEntrenamientoRepository sesionRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final EvaluacionDiariaRepository evaluacionRepository;
    private final CategoriaRepository categoriaRepository;
    private final HorarioService horarioService;

    /**
     * Sesiones de hoy. veTodasLasSesiones=true (ADMINISTRADOR/RECEPCIONISTA)
     * devuelve todas; en caso contrario se filtra por el entrenador
     * asociado al username, resuelto aqui y no en el controller porque
     * requiere consultar EntrenadorRepository.
     */
    @Transactional
    public List<SesionHoyResponse> sesionesDeHoy(String username, boolean veTodasLasSesiones) {
        horarioService.generarSesionesDeHoy();
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);

        List<SesionEntrenamiento> sesiones;
        if (veTodasLasSesiones) {
            sesiones = sesionRepository.findByFechaOrderByHoraInicioAsc(hoy);
        } else {
            Entrenador entrenador = entrenadorPorUsername(username);
            sesiones = entrenador == null
                    ? List.of()
                    : sesionRepository.findByFechaOrderByHoraInicioAsc(hoy).stream()
                        .filter(s -> s.getEntrenador().getIdEntrenador().equals(entrenador.getIdEntrenador()))
                        .toList();
        }

        return sesiones.stream().map(this::aResponse).toList();
    }

    /**
     * Historial completo (pasadas y futuras), no solo las de hoy: sin esto,
     * cualquier dia sin sesion programada dejaba al entrenador sin forma de
     * llegar a una evaluacion o plantilla pasada. Mismo criterio
     * veTodasLasSesiones que sesionesDeHoy(): un ADMINISTRADOR audita el
     * historial completo de todos los entrenadores, no solo el propio (que
     * ademas no tiene, al no ser un Entrenador).
     */
    @Transactional
    public List<SesionHoyResponse> misSesiones(String username, boolean veTodasLasSesiones, int page, int size) {
        horarioService.generarSesionesDeHoy();

        if (veTodasLasSesiones) {
            Page<SesionEntrenamiento> todas = sesionRepository.findAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha")));
            return todas.map(this::aResponse).getContent();
        }

        Entrenador entrenador = entrenadorPorUsername(username);
        if (entrenador == null) {
            return List.of();
        }

        Page<SesionEntrenamiento> pagina = sesionRepository.findByEntrenadorIdEntrenadorOrderByFechaDesc(
                entrenador.getIdEntrenador(), PageRequest.of(page, size));
        return pagina.map(this::aResponse).getContent();
    }

    /**
     * Alta de una sesion propia. El idEntrenador nunca viene del cliente: se
     * resuelve del username autenticado, para que un entrenador no pueda
     * crear una sesion "a nombre" de otro con solo cambiar un id en el body.
     */
    @Transactional
    public SesionHoyResponse crear(String username, SesionCrearRequest request) {
        Entrenador entrenador = entrenadorPorUsername(username);
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
        return aResponse(sesion);
    }

    private Entrenador entrenadorPorUsername(String username) {
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
