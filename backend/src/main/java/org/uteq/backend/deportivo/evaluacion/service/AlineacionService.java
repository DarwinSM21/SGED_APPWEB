package org.uteq.backend.deportivo.evaluacion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.dto.AlineacionDtos.*;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;
import org.uteq.backend.deportivo.evaluacion.entity.AlineacionJugador;
import org.uteq.backend.deportivo.evaluacion.repository.AlineacionRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La alineacion que el entrenador pone en cancha.
 *
 * <p>Convive con {@link PlantillaService}, que calcula la <b>sugerencia</b> a
 * partir de asistencia, lesiones y promedio. La regla es simple y es la que
 * hace que todo esto tenga sentido: <b>si el entrenador guardo una alineacion
 * se devuelve esa; si no, la sugerida</b>. Asi el historial sale gratis —
 * abrir una sesion pasada muestra el once con el que realmente se jugo — sin
 * duplicar el calculo ni congelar sugerencias que nadie llego a usar.
 *
 * <p>La sugerencia no se guarda nunca por su cuenta. Guardar automaticamente
 * lo que el sistema propone convertiria una recomendacion en un hecho
 * historico sin que nadie lo haya decidido, y despues no habria forma de
 * distinguir "el entrenador jugo con este once" de "el sistema lo propuso y
 * nadie miro".
 */
@Service
@RequiredArgsConstructor
public class AlineacionService {

    private final AlineacionRepository alineacionRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EstudianteRepository estudianteRepository;
    private final PosicionRepository posicionRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    private final LesionRepository lesionRepository;
    private final PlantillaService plantillaService;

    @Transactional(readOnly = true)
    public AlineacionResponse verDeSesion(Long idSesion) {
        SesionEntrenamiento sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesion " + idSesion));

        return alineacionRepository.findBySesion_IdSesion(idSesion)
                .map(a -> desdeGuardada(sesion, a))
                .orElseGet(() -> desdeSugerencia(sesion));
    }

    /**
     * Guarda el once que el entrenador decidio. Reemplaza por completo el
     * anterior en vez de mezclar: si el entrenador quita a alguien del campo,
     * conservar su fila "por si acaso" dejaria en la base a un jugador que el
     * entrenador saco a proposito.
     */
    @Transactional
    public AlineacionResponse guardar(Long idSesion, GuardarAlineacionRequest request) {
        SesionEntrenamiento sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesion " + idSesion));

        Set<Long> presentes = idsPresentes(idSesion);
        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());
        Set<Long> vistos = new HashSet<>();
        Map<Long, Long> puestoOcupado = new HashMap<>();

        // Se valida TODO antes de tocar la base. Si algo falla, la alineacion
        // anterior queda intacta: a mitad de una validacion no puede quedar un
        // once a medias.
        List<AlineacionJugador> nuevos = new ArrayList<>();
        for (JugadorEnCancha j : request.jugadores()) {
            if (!vistos.add(j.idEstudiante())) {
                throw new IllegalArgumentException("Un jugador no puede estar dos veces en la alineación");
            }

            Estudiante estudiante = estudianteRepository.findByIdEstudianteAndActivoTrue(j.idEstudiante())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Estudiante no encontrado o inactivo: " + j.idEstudiante()));
            String nombre = estudiante.getPersona().getNombre() + " " + estudiante.getPersona().getApellido();

            // Alinear a quien no estuvo en el entrenamiento no es un dato mas:
            // rompe la relacion entre lo que se midio y lo que se jugo, que es
            // justamente lo que este modulo existe para sostener.
            if (!presentes.contains(j.idEstudiante())) {
                throw new IllegalArgumentException(nombre + " no tiene asistencia registrada en esta sesión");
            }
            if (lesionados.contains(j.idEstudiante())) {
                throw new IllegalArgumentException(nombre + " arrastra una lesión activa y no puede alinearse");
            }

            Posicion posicion = null;
            if (j.idPosicion() != null) {
                posicion = posicionRepository.findById(j.idPosicion())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Posicion no encontrada: " + j.idPosicion()));

                if (Boolean.TRUE.equals(j.titular())
                        && puestoOcupado.put(j.idPosicion(), j.idEstudiante()) != null) {
                    throw new IllegalArgumentException(
                            "Dos titulares no pueden ocupar el puesto " + posicion.getAbreviatura());
                }
            }

            nuevos.add(AlineacionJugador.builder()
                    .estudiante(estudiante)
                    .posicion(posicion)
                    .titular(Boolean.TRUE.equals(j.titular()))
                    .build());
        }

        Alineacion alineacion = alineacionRepository.findBySesion_IdSesion(idSesion)
                .orElseGet(() -> Alineacion.builder().sesion(sesion).build());
        alineacion.setValoracion(request.valoracion());
        alineacion.setObservacion(request.observacion());

        // Vaciar y volver a llenar en el mismo flush hace que Hibernate intente
        // insertar antes de borrar y choque contra uq_alineacion_jugador. El
        // saveAndFlush intermedio fuerza que los DELETE salgan primero.
        if (!alineacion.getJugadores().isEmpty()) {
            alineacion.getJugadores().clear();
            alineacionRepository.saveAndFlush(alineacion);
        }
        for (AlineacionJugador j : nuevos) {
            j.setAlineacion(alineacion);
            alineacion.getJugadores().add(j);
        }

        alineacionRepository.save(alineacion);
        return verDeSesion(idSesion);
    }

    /** Vuelve a la sugerencia del sistema, descartando los cambios manuales. */
    @Transactional
    public AlineacionResponse restablecer(Long idSesion) {
        alineacionRepository.findBySesion_IdSesion(idSesion).ifPresent(alineacionRepository::delete);
        return verDeSesion(idSesion);
    }

    // ------------------------------------------------------------------

    private AlineacionResponse desdeGuardada(SesionEntrenamiento sesion, Alineacion a) {
        List<Estudiante> enCancha = a.getJugadores().stream()
                .map(AlineacionJugador::getEstudiante).toList();
        Map<Long, BigDecimal> promedios = promediosDe(enCancha);

        List<JugadorAlineadoResponse> titulares = new ArrayList<>();
        List<JugadorAlineadoResponse> suplentes = new ArrayList<>();
        Set<Long> yaEstan = new HashSet<>();

        for (AlineacionJugador j : a.getJugadores()) {
            yaEstan.add(j.getEstudiante().getIdEstudiante());
            var fila = aResponse(j.getEstudiante(), j.getPosicion(),
                    Boolean.TRUE.equals(j.getTitular()), promedios);
            (Boolean.TRUE.equals(j.getTitular()) ? titulares : suplentes).add(fila);
        }

        return new AlineacionResponse(
                sesion.getIdSesion(), sesion.getCategoria().getNombre(), sesion.getFecha(),
                true, a.getValoracion(), a.getObservacion(),
                titulares, suplentes, disponibles(sesion.getIdSesion(), yaEstan));
    }

    /**
     * Adapta lo que ya calcula PlantillaService al mismo formato, para que la
     * pantalla no tenga que distinguir entre "sugerida" y "guardada" mas alla
     * de la bandera. El calculo no se duplica.
     */
    private AlineacionResponse desdeSugerencia(SesionEntrenamiento sesion) {
        var sugerida = plantillaService.sugerir(sesion.getIdSesion());
        Set<Long> yaEstan = new HashSet<>();

        // La sugerencia identifica la posicion por su abreviatura, pero para
        // poder GUARDARLA despues hace falta el id: sin el, la pantalla
        // reenviaria posiciones nulas y el once perderia sus puestos al
        // primer guardado.
        Map<String, Long> idPorAbreviatura = new HashMap<>();
        for (Posicion p : posicionRepository.findByActivoTrueOrderByIdPosicionAsc()) {
            idPorAbreviatura.put(p.getAbreviatura(), p.getIdPosicion());
        }

        List<JugadorAlineadoResponse> titulares = new ArrayList<>();
        for (var t : sugerida.titulares()) {
            yaEstan.add(t.idEstudiante());
            titulares.add(new JugadorAlineadoResponse(t.idEstudiante(), t.nombreCompleto(),
                    t.posicion(), idPorAbreviatura.get(t.posicion()), true, t.promedioAcumulado()));
        }
        List<JugadorAlineadoResponse> suplentes = new ArrayList<>();
        for (var s : sugerida.suplentes()) {
            yaEstan.add(s.idEstudiante());
            suplentes.add(new JugadorAlineadoResponse(s.idEstudiante(), s.nombreCompleto(),
                    s.posicion(), idPorAbreviatura.get(s.posicion()), false, s.promedioAcumulado()));
        }

        return new AlineacionResponse(
                sesion.getIdSesion(), sesion.getCategoria().getNombre(), sesion.getFecha(),
                false, null, null, titulares, suplentes,
                disponibles(sesion.getIdSesion(), yaEstan));
    }

    /**
     * Quienes asistieron, no estan lesionados y no figuran en el once: es de
     * donde salen los cambios. Sin esta lista el entrenador no tendria a quien
     * meter.
     */
    private List<JugadorAlineadoResponse> disponibles(Long idSesion, Set<Long> yaEstan) {
        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());
        List<Estudiante> libres = asistenciaRepository.listarHabilitadosParaEvaluar(idSesion).stream()
                .map(a -> a.getEstudiante())
                .filter(e -> !yaEstan.contains(e.getIdEstudiante()))
                .filter(e -> !lesionados.contains(e.getIdEstudiante()))
                .toList();

        Map<Long, BigDecimal> promedios = promediosDe(libres);
        return libres.stream()
                .map(e -> aResponse(e, e.getPosicion(), false, promedios))
                .toList();
    }

    private JugadorAlineadoResponse aResponse(Estudiante e, Posicion posicion, boolean titular,
                                              Map<Long, BigDecimal> promedios) {
        return new JugadorAlineadoResponse(
                e.getIdEstudiante(),
                e.getPersona().getNombre() + " " + e.getPersona().getApellido(),
                posicion == null ? null : posicion.getAbreviatura(),
                posicion == null ? null : posicion.getIdPosicion(),
                titular,
                promedios.getOrDefault(e.getIdEstudiante(), BigDecimal.ZERO));
    }

    private Map<Long, BigDecimal> promediosDe(List<Estudiante> estudiantes) {
        if (estudiantes.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = estudiantes.stream().map(Estudiante::getIdEstudiante).toList();
        Map<Long, BigDecimal> promedios = new HashMap<>();
        for (Object[] fila : evaluacionEstudianteRepository.promedioGeneralPorEstudiante(ids)) {
            Long id = (Long) fila[0];
            double valor = fila[1] == null ? 0.0 : ((Number) fila[1]).doubleValue();
            promedios.put(id, BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP));
        }
        return promedios;
    }

    private Set<Long> idsPresentes(Long idSesion) {
        Set<Long> presentes = new HashSet<>();
        for (var a : asistenciaRepository.listarHabilitadosParaEvaluar(idSesion)) {
            presentes.add(a.getEstudiante().getIdEstudiante());
        }
        return presentes;
    }
}
