package org.uteq.backend.deportivo.horario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.horario.dto.HorarioRequest;
import org.uteq.backend.deportivo.horario.dto.HorarioResponse;
import org.uteq.backend.deportivo.horario.entity.Horario;
import org.uteq.backend.deportivo.horario.repository.HorarioRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionDiariaRepository;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioService {
    private final HorarioRepository horarioRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final CategoriaRepository categoriaRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EvaluacionDiariaRepository evaluacionRepository;

    @Value("${sesiones.dias-programados:7}")
    private int diasProgramados;

    private static final Long SIN_ID_TODAVIA = -1L;

    @Transactional
    public HorarioResponse crear(String username, HorarioRequest request) {
        Entrenador entrenador = entrenadorAutenticado(username);

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: " + request.idCategoria()));

        validarQueNoSeCruce(entrenador.getIdEntrenador(), request, SIN_ID_TODAVIA);

        Horario horario = Horario.builder()
                .entrenador(entrenador)
                .categoria(categoria)
                .diaSemana(request.diaSemana().shortValue())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .campo(request.campo())
                .descripcion(request.descripcion())
                .activo(true)
                .build();

        return aResponse(horarioRepository.save(horario));
    }

    private void validarQueNoSeCruce(Long idEntrenador, HorarioRequest request, Long idExcluir) {
        List<Horario> choques = horarioRepository.cruzadosCon(
                idEntrenador, request.diaSemana().shortValue(),
                request.horaInicio(), request.horaFin(), idExcluir);
        if (choques.isEmpty()) {
            return;
        }
        Horario otro = choques.get(0);
        throw new IllegalArgumentException(
                "Ese día ya tenés " + otro.getCategoria().getNombre() + " de "
                        + otro.getHoraInicio() + " a " + otro.getHoraFin()
                        + ". No podés estar en dos canchas a la vez: movelo de hora.");
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> misHorarios(String username) {
        return entrenadorRepository.findByUsuario_Username(username)
                .map(entrenador -> {
                    List<Horario> horarios = horarioRepository
                            .findByEntrenador_IdEntrenadorAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(
                                    entrenador.getIdEntrenador());

                    return horarios.stream().map(h -> aResponse(h, choqueDe(h, horarios))).toList();
                })
                .orElseGet(List::of);
    }

    private String choqueDe(Horario horario, List<Horario> todos) {
        return todos.stream()
                .filter(o -> !o.getIdHorario().equals(horario.getIdHorario()))
                .filter(o -> o.getDiaSemana().equals(horario.getDiaSemana()))
                .filter(o -> o.getHoraInicio().isBefore(horario.getHoraFin())
                        && o.getHoraFin().isAfter(horario.getHoraInicio()))
                .findFirst()
                .map(o -> o.getCategoria().getNombre() + " (" + o.getHoraInicio()
                        + "–" + o.getHoraFin() + ")")
                .orElse(null);
    }

    @Transactional
    public void desactivar(String username, Long idHorario) {
        Entrenador entrenador = entrenadorAutenticado(username);
        Horario horario = horarioRepository
                .findByIdHorarioAndEntrenador_IdEntrenador(idHorario, entrenador.getIdEntrenador())
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado con id: " + idHorario));
        horario.setActivo(false);
        horarioRepository.save(horario);
    }

    @Transactional
    public HorarioResponse editar(String username, Long idHorario, HorarioRequest request) {
        Entrenador entrenador = entrenadorAutenticado(username);
        Horario horario = horarioRepository
                .findByIdHorarioAndEntrenador_IdEntrenador(idHorario, entrenador.getIdEntrenador())
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado con id: " + idHorario));

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: " + request.idCategoria()));

        validarQueNoSeCruce(entrenador.getIdEntrenador(), request, idHorario);

        horario.setCategoria(categoria);
        horario.setDiaSemana(request.diaSemana().shortValue());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setCampo(request.campo());
        horario.setDescripcion(request.descripcion());
        horarioRepository.save(horario);

        rehacerSesionesFuturas(horario);
        return aResponse(horario);
    }

    private void rehacerSesionesFuturas(Horario horario) {
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);

        for (SesionEntrenamiento sesion : sesionRepository
                .findByHorario_IdHorarioAndFechaGreaterThanEqual(horario.getIdHorario(), hoy)) {
            boolean tieneAsistencia = !asistenciaRepository.findBySesionIdSesion(sesion.getIdSesion()).isEmpty();
            boolean tieneEvaluacion = evaluacionRepository.existsBySesionIdSesion(sesion.getIdSesion());
            if (tieneAsistencia || tieneEvaluacion) {
                continue;
            }
            sesionRepository.delete(sesion);
        }

        generarSesionesProgramadas();
    }

    @Transactional
    public void generarSesionesProgramadas() {
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);

        for (int desplazamiento = 0; desplazamiento <= Math.max(0, diasProgramados); desplazamiento++) {
            LocalDate fecha = hoy.plusDays(desplazamiento);
            short diaSemana = (short) fecha.getDayOfWeek().getValue();

            for (Horario horario : horarioRepository.findByActivoTrueAndDiaSemana(diaSemana)) {
                if (sesionRepository.existsByHorario_IdHorarioAndFecha(horario.getIdHorario(), fecha)) {
                    continue;
                }
                sesionRepository.save(SesionEntrenamiento.builder()
                        .horario(horario)
                        .entrenador(horario.getEntrenador())
                        .categoria(horario.getCategoria())
                        .fecha(fecha)
                        .horaInicio(horario.getHoraInicio())
                        .horaFin(horario.getHoraFin())
                        .campo(horario.getCampo())
                        .estado("PROGRAMADA")
                        .build());
            }
        }
    }

    private Entrenador entrenadorAutenticado(String username) {
        return entrenadorRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un entrenador asociado a esta cuenta"));
    }

    private HorarioResponse aResponse(Horario h) {
        return aResponse(h, null);
    }

    private HorarioResponse aResponse(Horario h, String chocaCon) {
        return new HorarioResponse(
                h.getIdHorario(), h.getCategoria().getIdCategoria(), h.getCategoria().getNombre(),
                h.getDiaSemana().intValue(),
                h.getHoraInicio(), h.getHoraFin(), h.getCampo(), h.getDescripcion(), h.getActivo(),
                chocaCon);
    }
}
