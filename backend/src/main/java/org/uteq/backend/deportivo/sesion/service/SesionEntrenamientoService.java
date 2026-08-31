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
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.horario.service.HorarioService;
import org.uteq.backend.deportivo.sesion.dto.SesionCrearRequest;
import org.uteq.backend.deportivo.sesion.dto.SesionHistorialResponse;
import org.uteq.backend.deportivo.sesion.dto.SesionHoyResponse;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SesionEntrenamientoService {
    private final SesionEntrenamientoRepository sesionRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final EvaluacionDiariaRepository evaluacionRepository;
    private final CategoriaRepository categoriaRepository;
    private final HorarioService horarioService;
    private final AsistenciaRepository asistenciaRepository;
    private final EstudianteRepository estudianteRepository;

    @Transactional
    public List<SesionHoyResponse> sesionesDeHoy(String username, boolean veTodasLasSesiones) {
        horarioService.generarSesionesProgramadas();
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

    @Transactional
    public List<SesionHoyResponse> misSesiones(String username, boolean veTodasLasSesiones, int page, int size) {
        horarioService.generarSesionesProgramadas();

        if (veTodasLasSesiones) {
            Page<SesionEntrenamiento> todas = sesionRepository.findAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha")));
            return todas.map(this::aResponse).getContent();
        }

        Entrenador entrenador = entrenadorPorUsername(username);
        if (entrenador == null) {
            return List.of();
        }

        Page<SesionEntrenamiento> pagina = sesionRepository.sesionesDelEntrenador(
                entrenador.getIdEntrenador(), PageRequest.of(page, size));
        return pagina.map(this::aResponse).getContent();
    }

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

        if (sesionRepository.existeSolape(request.idCategoria(), request.fecha(),
                                          request.horaInicio(), request.horaFin())) {
            throw new IllegalArgumentException(
                    "Ya hay una sesión de esa categoría ese día en ese horario");
        }

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

    @Transactional(readOnly = true)
    public SesionHistorialResponse historial(Long idSesion) {
        SesionEntrenamiento s = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesion " + idSesion));

        Map<Long, Asistencia> porEstudiante = new HashMap<>();
        for (Asistencia a : asistenciaRepository.historialDeSesion(idSesion)) {
            porEstudiante.put(a.getEstudiante().getIdEstudiante(), a);
        }

        List<Estudiante> plantel = estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(
                        s.getCategoria().getIdCategoria());

        List<SesionHistorialResponse.FilaAsistencia> filas = new ArrayList<>();
        int presentes = 0, tarde = 0, ausentes = 0, justificados = 0, sinRegistro = 0;
        for (Estudiante e : plantel) {
            Asistencia a = porEstudiante.remove(e.getIdEstudiante());
            String estado = a == null ? "SIN_REGISTRO" : a.getEstado();
            switch (estado) {
                case Asistencia.ESTADO_PRESENTE -> presentes++;
                case Asistencia.ESTADO_TARDE -> tarde++;
                case Asistencia.ESTADO_AUSENTE -> ausentes++;
                case Asistencia.ESTADO_JUSTIFICADO -> justificados++;
                default -> sinRegistro++;
            }
            filas.add(new SesionHistorialResponse.FilaAsistencia(
                    e.getIdEstudiante(),
                    e.getPersona().getNombre() + " " + e.getPersona().getApellido(),
                    e.getPosicion() == null ? null : e.getPosicion().getAbreviatura(),
                    estado,
                    a == null ? null : a.getHoraEntrada(),
                    a == null ? null : a.getMetodo(),
                    a == null ? null : a.getObservacion()));
        }

        for (Asistencia a : porEstudiante.values()) {
            Estudiante e = a.getEstudiante();
            switch (a.getEstado()) {
                case Asistencia.ESTADO_PRESENTE -> presentes++;
                case Asistencia.ESTADO_TARDE -> tarde++;
                case Asistencia.ESTADO_AUSENTE -> ausentes++;
                case Asistencia.ESTADO_JUSTIFICADO -> justificados++;
                default -> { }
            }
            filas.add(new SesionHistorialResponse.FilaAsistencia(
                    e.getIdEstudiante(),
                    e.getPersona().getNombre() + " " + e.getPersona().getApellido(),
                    e.getPosicion() == null ? null : e.getPosicion().getAbreviatura(),
                    a.getEstado(), a.getHoraEntrada(), a.getMetodo(), a.getObservacion()));
        }

        var evaluacion = evaluacionRepository.findBySesionIdSesion(idSesion);
        var persona = s.getEntrenador().getPersona();
        return new SesionHistorialResponse(
                s.getIdSesion(),
                s.getCategoria().getNombre(),
                persona.getNombre() + " " + persona.getApellido(),
                s.getFecha(), s.getHoraInicio(), s.getHoraFin(), s.getCampo(), s.getEstado(),
                evaluacion.isPresent(),
                evaluacion.map(ev -> ev.getEstado()).orElse(null),
                new SesionHistorialResponse.Resumen(filas.size(), presentes, tarde,
                        ausentes, justificados, sinRegistro),
                filas);
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
