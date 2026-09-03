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

/**
 * Horarios fijos semanales del entrenador y la materialización de las
 * sesiones concretas que se derivan de ellos. Un entrenador no puede tener
 * dos horarios cruzados el mismo día; la cancha no se valida (dos grupos
 * pueden compartirla, una persona no se parte en dos). Al cambiar un
 * horario, la semana en curso se rehace salvo los entrenamientos que ya se
 * dictaron (tienen asistencia o evaluación).
 */
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
     * Cuántos días hacia adelante se programan de una vez. Con 7 basta una
     * sola apertura de la pantalla para dejar cubierta la semana completa.
     */
    @Value("${sesiones.dias-programados:7}")
    private int diasProgramados;

    /** Id imposible, para el alta: no hay horario propio que excluir todavía. */
    private static final Long SIN_ID_TODAVIA = -1L;

    /**
     * Crea un horario fijo para el entrenador autenticado.
     *
     * @param username nombre de usuario del entrenador
     * @param request  categoría, día de semana, franja horaria y campo
     * @return el horario creado
     * @throws RecursoNoEncontradoException si la cuenta no tiene entrenador
     *                                      asociado o la categoría no existe
     * @throws IllegalArgumentException     si la hora de fin no es posterior
     *                                      a la de inicio, o el horario se
     *                                      cruza con otro suyo el mismo día
     */
    @Transactional
    public HorarioResponse crear(String username, HorarioRequest request) {
        Entrenador entrenador = entrenadorAutenticado(username);

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: " + request.idCategoria()));

        // SIN_ID_TODAVIA porque el horario aún no existe: no hay nada que excluir.
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
     * Un entrenador no puede tener dos horarios cruzados el mismo día. Un
     * choque en el horario se materializa una vez por semana durante meses.
     * La cancha no se valida (dos grupos pueden compartirla). El mensaje
     * nombra el horario con el que choca.
     *
     * @param idEntrenador entrenador dueño de los horarios
     * @param request      horario que se pretende crear o editar
     * @param idExcluir    horario a excluir de la comprobación (el que se
     *                     edita), o {@link #SIN_ID_TODAVIA} en un alta
     * @throws IllegalArgumentException si hay un cruce
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

    /**
     * Horarios activos del entrenador autenticado, cada uno con la
     * descripción del primer horario suyo que se le cruza (o {@code null}).
     *
     * @param username nombre de usuario del entrenador
     * @return la lista de horarios; vacía si la cuenta no tiene entrenador
     */
    @Transactional(readOnly = true)
    public List<HorarioResponse> misHorarios(String username) {
        return entrenadorRepository.findByUsuario_Username(username)
                .map(entrenador -> {
                    List<Horario> horarios = horarioRepository
                            .findByEntrenador_IdEntrenadorAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(
                                    entrenador.getIdEntrenador());

                    // Se comparan en memoria y no con una consulta por fila: la
                    // semana de un entrenador son unos pocos horarios.
                    return horarios.stream().map(h -> aResponse(h, choqueDe(h, horarios))).toList();
                })
                .orElseGet(List::of);
    }

    // El primer horario del mismo día que se cruza con este, descrito para
    // mostrarlo. null si no hay choque.
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

    /**
     * Desactiva un horario fijo del entrenador autenticado. Responde
     * {@code 404} uniforme si no existe o no es suyo (criterio IDOR).
     *
     * @param username  nombre de usuario del entrenador
     * @param idHorario identificador del horario
     * @throws RecursoNoEncontradoException si el horario no existe o no es
     *                                      del entrenador
     */
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
     * Cambia un horario fijo del entrenador y rehace su ventana de sesiones.
     * Responde {@code 404} uniforme si no existe o no es suyo.
     *
     * @param username  nombre de usuario del entrenador
     * @param idHorario identificador del horario
     * @param request   datos nuevos
     * @return el horario actualizado
     * @throws RecursoNoEncontradoException si el horario no existe o no es
     *                                      suyo, o la categoría no existe
     * @throws IllegalArgumentException     si la franja es inválida o se
     *                                      cruza con otro horario suyo
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

        // Se excluye a sí mismo: mover un horario media hora no es chocar consigo.
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

    // Vuelve a materializar la ventana de este horario tras un cambio. Solo
    // se borran las sesiones que aún no ocurrieron Y en las que nadie
    // registró nada: una sesión con asistencia o evaluación se queda como
    // está, son hechos que ya pasaron.
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
     * activos, desde hoy y hasta {@code sesiones.dias-programados} días hacia
     * adelante. Idempotente a propósito: se llama en cada
     * {@code GET /api/sesiones/hoy} y {@code /mias}, y si la sesión de ese
     * horario ya existe para esa fecha no crea otra. No se generan fechas
     * pasadas: una sesión creada después de su día, sin asistencia ni
     * evaluación, se leería como un entrenamiento al que no fue nadie.
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
