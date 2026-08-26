package org.uteq.backend.deportivo.partido.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.common.ia.GeneradorFeedbackIA;
import org.uteq.backend.common.ia.PerfilJugadorAnonimo;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.JugadorConvocado;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.NoConvocable;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.VentanaRendimiento;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.repository.PartidoRepository;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sugerencia de once para un partido.
 *
 * <p><b>La IA no elige a los jugadores.</b> La seleccion sale de una regla
 * explicita y reproducible a mano con los mismos datos:
 *
 * <ol>
 *   <li>El universo es el plantel activo de la categoria que juega.</li>
 *   <li>Queda fuera quien arrastra una lesion activa y quien no piso un solo
 *       entrenamiento en la ventana. Los dos se muestran con el motivo: que
 *       alguien desaparezca de la lista sin explicacion es peor que no
 *       listarlo.</li>
 *   <li>Se ordena por el promedio de las ultimas semanas -no el historico
 *       completo-, desempatando por presencias y despues por id, para que dos
 *       llamadas con los mismos datos devuelvan lo mismo.</li>
 *   <li>Se titulariza al mejor de cada posicion nominal, no a los once mejores
 *       promedios: eso ultimo podia sugerir dos porteros y ningun defensa.</li>
 * </ol>
 *
 * <p>La ventana es lo que hace que la sugerencia se alimente semana a semana.
 * El promedio historico completo premia al que jugo bien hace un anio por
 * encima del que viene mejor ahora, que es lo contrario de lo que hace falta
 * para decidir con quien se sale el sabado.
 *
 * <p>El modelo de lenguaje solo redacta un comentario sobre un once ya
 * decidido, y solo cuando se le pide. Dejarle decidir quien juega haria la
 * decision inauditable e imposible de explicarle a un padre que pregunta por
 * que su hijo quedo fuera.
 */
@Service
@RequiredArgsConstructor
public class ConvocatoriaService {

    private final PartidoRepository partidoRepository;
    private final EstudianteRepository estudianteRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final LesionRepository lesionRepository;
    private final GeneradorFeedbackIA generadorFeedback;

    @Value("${plantilla.titulares:11}")
    private int cantidadTitulares;

    /** Cuantas semanas hacia atras se miran. Cuatro es un mes de entrenamientos. */
    @Value("${plantilla.semanas-rendimiento:4}")
    private int semanasRendimiento;

    public int cupoTitulares() {
        return cantidadTitulares;
    }

    /**
     * Convocatoria calculada para un partido. Es la SUGERENCIA: no se guarda
     * sola. Guardar automaticamente lo que el sistema propone convertiria una
     * recomendacion en un hecho historico sin que nadie lo decidiera, y
     * despues no habria forma de distinguir "el entrenador jugo con este once"
     * de "el sistema lo propuso y nadie miro".
     */
    @Transactional(readOnly = true)
    public Convocatoria calcular(Long idPartido) {
        Partido partido = partidoRepository.findWithCategoriaByIdPartido(idPartido)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el partido " + idPartido));
        return calcular(partido);
    }

    @Transactional(readOnly = true)
    public Convocatoria calcular(Partido partido) {
        Long idCategoria = partido.getCategoria().getIdCategoria();
        LocalDate hasta = partido.getFecha();
        LocalDate desde = hasta.minusWeeks(semanasRendimiento);

        long entrenamientos = sesionRepository
                .countByCategoriaIdCategoriaAndFechaBetween(idCategoria, desde, hasta);
        VentanaRendimiento ventana =
                new VentanaRendimiento(semanasRendimiento, desde, hasta, entrenamientos);

        List<Estudiante> plantel = estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(idCategoria);
        if (plantel.isEmpty()) {
            return new Convocatoria(partido, ventana, List.of(), List.of(), List.of(),
                    Map.of(), Map.of(), entrenamientos);
        }

        List<Long> ids = plantel.stream().map(Estudiante::getIdEstudiante).toList();
        Map<Long, BigDecimal> promedios = promediosDe(ids, desde, hasta);
        Map<Long, Long> presencias = presenciasDe(ids, desde, hasta);
        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());

        List<Estudiante> convocables = new ArrayList<>();
        List<NoConvocable> fuera = new ArrayList<>();
        for (Estudiante e : plantel) {
            Long id = e.getIdEstudiante();
            if (lesionados.contains(id)) {
                fuera.add(new NoConvocable(id, nombreDe(e), "Lesión activa"));
            } else if (entrenamientos > 0 && presencias.getOrDefault(id, 0L) == 0L) {
                // Si la categoria no tuvo entrenamientos en la ventana nadie
                // pudo asistir, y castigar por eso a todo el plantel dejaria al
                // entrenador sin nadie a quien alinear.
                fuera.add(new NoConvocable(id, nombreDe(e),
                        "No entrenó en las últimas " + semanasRendimiento + " semanas"));
            } else {
                convocables.add(e);
            }
        }

        convocables.sort(porRendimiento(promedios, presencias));

        // Un titular por posicion nominal. convocables ya viene ordenado
        // mejor-a-peor, asi que el primero de cada puesto es por construccion
        // el mejor disponible para ese puesto; el resto de esa misma posicion
        // -y quien no tiene posicion registrada, que no puede llenar ningun
        // puesto- cae a suplente.
        Map<Long, JugadorConvocado> titularPorPuesto = new LinkedHashMap<>();
        List<JugadorConvocado> suplentes = new ArrayList<>();
        for (Estudiante e : convocables) {
            Long idPosicion = e.getPosicion() == null ? null : e.getPosicion().getIdPosicion();
            boolean hayCupo = titularPorPuesto.size() < cantidadTitulares;
            boolean titulariza = idPosicion != null && hayCupo && !titularPorPuesto.containsKey(idPosicion);
            JugadorConvocado fila =
                    aConvocado(e, idPosicion, titulariza, promedios, presencias, entrenamientos);
            if (titulariza) {
                titularPorPuesto.put(idPosicion, fila);
            } else {
                suplentes.add(fila);
            }
        }

        return new Convocatoria(partido, ventana,
                new ArrayList<>(titularPorPuesto.values()), suplentes, fuera,
                promedios, presencias, entrenamientos);
    }

    /**
     * Comentario del modelo sobre un once ya decidido. Se pide a demanda y no
     * en cada apertura de pantalla: gastar cuota de un servicio externo sin
     * que nadie lo haya pedido no le sirve a nadie.
     *
     * <p>Solo se envian datos seudonimizados: "Jugador 1", su categoria, su
     * puesto y sus numeros. Ningun nombre sale del sistema.
     */
    public GeneradorFeedbackIA.ResultadoFeedback comentar(
            List<JugadorConvocado> titulares, String categoria) {

        List<PerfilJugadorAnonimo> perfiles = new ArrayList<>();
        for (int i = 0; i < titulares.size(); i++) {
            JugadorConvocado t = titulares.get(i);
            double promedio = t.promedio() == null ? 0.0 : t.promedio().doubleValue();
            perfiles.add(new PerfilJugadorAnonimo(
                    "Jugador " + (i + 1), categoria, t.posicion(),
                    Map.of("Promedio acumulado", promedio,
                            "Entrenamientos asistidos", (double) t.presencias()),
                    Map.of(), null, false));
        }
        return generadorFeedback.generarComentarioPlantilla(perfiles);
    }

    // ------------------------------------------------------------------

    /**
     * Promedio primero, presencias despues, id al final. El desempate por id
     * no es cosmetico: sin el, dos llamadas con los mismos datos podrian
     * devolver onces distintos.
     */
    private Comparator<Estudiante> porRendimiento(Map<Long, BigDecimal> promedios,
                                                  Map<Long, Long> presencias) {
        return Comparator
                .comparing((Estudiante e) -> promedios.getOrDefault(
                        e.getIdEstudiante(), BigDecimal.ZERO)).reversed()
                .thenComparing(Comparator.comparingLong(
                        (Estudiante e) -> presencias.getOrDefault(e.getIdEstudiante(), 0L)).reversed())
                .thenComparing(Estudiante::getIdEstudiante);
    }

    private Map<Long, BigDecimal> promediosDe(List<Long> ids, LocalDate desde, LocalDate hasta) {
        Map<Long, BigDecimal> promedios = new HashMap<>();
        for (Object[] fila : evaluacionEstudianteRepository.promedioEnVentana(ids, desde, hasta)) {
            if (fila[1] == null) {
                continue;
            }
            promedios.put((Long) fila[0], BigDecimal.valueOf(((Number) fila[1]).doubleValue())
                    .setScale(1, RoundingMode.HALF_UP));
        }
        return promedios;
    }

    private Map<Long, Long> presenciasDe(List<Long> ids, LocalDate desde, LocalDate hasta) {
        Map<Long, Long> presencias = new HashMap<>();
        for (Object[] fila : asistenciaRepository.presenciasEnVentana(ids, desde, hasta)) {
            presencias.put((Long) fila[0], ((Number) fila[1]).longValue());
        }
        return presencias;
    }

    /**
     * @param idPosicion puesto de ESE partido, que no tiene por que ser la
     *                   posicion nominal del estudiante: de eso se trata poder
     *                   mover gente.
     */
    public JugadorConvocado aConvocado(Estudiante e, Long idPosicion, boolean titular,
                                       Map<Long, BigDecimal> promedios, Map<Long, Long> presencias,
                                       long entrenamientos) {
        String abreviatura = null;
        if (idPosicion != null) {
            var nominal = e.getPosicion();
            abreviatura = nominal != null && idPosicion.equals(nominal.getIdPosicion())
                    ? nominal.getAbreviatura() : null;
        }
        return new JugadorConvocado(
                e.getIdEstudiante(), nombreDe(e), abreviatura, idPosicion, titular,
                // null y no 0.0: "no lo evaluaron" y "sacó cero" son cosas
                // distintas y en pantalla se leen distinto.
                promedios.get(e.getIdEstudiante()),
                presencias.getOrDefault(e.getIdEstudiante(), 0L),
                entrenamientos);
    }

    public static String nombreDe(Estudiante e) {
        return e.getPersona().getNombre() + " " + e.getPersona().getApellido();
    }

    /**
     * Resultado del calculo. Lleva ademas los promedios y presencias ya
     * consultados para que quien tenga que rearmar filas -la alineacion
     * guardada, los disponibles- no vuelva a golpear la base con las mismas
     * dos consultas.
     */
    public record Convocatoria(
            Partido partido,
            VentanaRendimiento ventana,
            List<JugadorConvocado> titulares,
            List<JugadorConvocado> suplentes,
            List<NoConvocable> noConvocables,
            Map<Long, BigDecimal> promedios,
            Map<Long, Long> presencias,
            long entrenamientos
    ) {}
}
