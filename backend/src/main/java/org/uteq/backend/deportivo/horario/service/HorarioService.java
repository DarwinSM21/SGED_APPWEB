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

    /**
     * Cuantos dias hacia adelante se programan de una vez. Con 7 basta una
     * sola apertura de la pantalla para dejar cubierta la semana completa.
     */
    @Value("${sesiones.dias-programados:7}")
    private int diasProgramados;

    /** Id imposible, para el alta: no hay horario propio que excluir todavia. */
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

        // SIN_ID_TODAVIA porque el horario aun no existe: no hay nada que excluir.
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

    /**
     * Un entrenador no puede tener dos horarios cruzados el mismo dia.
     *
     * <p>Es la validacion que faltaba, y faltaba en el peor lugar. Un choque
     * en una sesion suelta es un dia mal cargado; un choque en el HORARIO se
     * materializa una vez por semana durante meses. En esta base habia tres
     * horarios del mismo entrenador a las 16:00 -SUB-12, SUB-14 y SUB-16, de
     * martes a viernes-, que generaron 248 sesiones imposibles: la persona no
     * puede estar en tres canchas, asi que dos de cada tres quedaban sin lista
     * y el sistema le cobraba esa ausencia a los chicos.
     *
     * <p>La cancha NO se valida. Dos grupos pueden compartirla; una persona no
     * se parte en dos. Bloquear la cancha impediria registrar algo que en la
     * escuela pasa de verdad.
     *
     * <p>El mensaje nombra el horario con el que choca. Con pocos entrenadores
     * -uno cubre varias categorias- reorganizar la semana hace saltar esto a
     * menudo, y un "no se puede" a secas obliga a ir a buscar cual era.
     */
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
                    // Se comparan en memoria y no con una consulta por fila: la
                    // semana de un entrenador son unos pocos horarios, y asi la
                    // pantalla no dispara un N+1 por marcar un aviso.
                    return horarios.stream().map(h -> aResponse(h, choqueDe(h, horarios))).toList();
                })
                .orElseGet(List::of);
    }

    /**
     * El primer horario del mismo dia que se cruza con este, descrito para
     * mostrarlo. null si no hay choque.
     */
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

    /** 404 uniforme si el horario no existe o no es suyo: mismo criterio IDOR del resto de la app. */
    @Transactional
    public void desactivar(String username, Long idHorario) {
        Entrenador entrenador = entrenadorAutenticado(username);
        Horario horario = horarioRepository
                .findByIdHorarioAndEntrenador_IdEntrenador(idHorario, entrenador.getIdEntrenador())
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado con id: " + idHorario));
        horario.setActivo(false);
        horarioRepository.save(horario);
    }

    /**
     * Cambia un horario fijo del entrenador.
     *
     * <p>Antes solo se podia crear y dar de baja: corregir "entreno a las 16
     * y no a las 15" obligaba a borrar el horario y volver a escribirlo
     * entero, y las sesiones ya generadas de esa semana se quedaban con la
     * hora vieja.
     *
     * <p>404 uniforme si no existe o no es suyo, mismo criterio IDOR que
     * desactivar().
     */
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

        // Se excluye a si mismo: mover un horario media hora no es chocar consigo.
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

    /**
     * Vuelve a materializar la ventana de este horario tras un cambio.
     *
     * <p>Solo se borran las sesiones que aun no ocurrieron Y en las que nadie
     * registro nada. Una sesion con asistencia o con evaluacion se queda como
     * esta aunque el horario haya cambiado: son hechos que ya pasaron, y
     * moverlos de hora reescribiria el historial de a que entrenamiento fue
     * cada estudiante. Es el mismo criterio por el que un pago se anula en
     * vez de editarse.
     *
     * <p>Consecuencia practica: al cambiar el dia o la hora, la semana en
     * curso se rehace salvo los entrenamientos que ya se dictaron.
     */
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

    /**
     * Materializa las sesiones que faltan a partir de los horarios fijos
     * activos, desde hoy y hasta {@code sesiones.dias-programados} dias
     * hacia adelante. Idempotente a proposito: se llama en cada
     * GET /api/sesiones/hoy y /mias (ver SesionEntrenamientoController), y
     * si la sesion de ese horario ya existe para esa fecha no crea otra.
     *
     * <p>Antes solo generaba la del dia en curso, y solo si alguien abria la
     * pantalla ese dia: con el sistema apagado de lunes a jueves, al abrirlo
     * el viernes esos cuatro dias no existian y el entrenador no tenia donde
     * registrar nada. Programar una ventana hacia adelante hace que una sola
     * apertura deje cubierta la semana entera.
     *
     * <p>Deliberadamente no se generan fechas pasadas: una sesion creada
     * despues de su dia, sin asistencia ni evaluacion, se leeria como un
     * entrenamiento al que no fue nadie. El horario dice lo que va a pasar,
     * no reconstruye lo que ya paso.
     */
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
