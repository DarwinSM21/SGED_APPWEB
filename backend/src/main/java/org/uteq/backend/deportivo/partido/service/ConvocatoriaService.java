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

    @Value("${plantilla.semanas-rendimiento:4}")
    private int semanasRendimiento;

    public int cupoTitulares() {
        return cantidadTitulares;
    }

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
                fuera.add(new NoConvocable(id, nombreDe(e),
                        "No entrenó en las últimas " + semanasRendimiento + " semanas"));
            } else {
                convocables.add(e);
            }
        }

        convocables.sort(porRendimiento(promedios, presencias));

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
                promedios.get(e.getIdEstudiante()),
                presencias.getOrDefault(e.getIdEstudiante(), 0L),
                entrenamientos);
    }

    public static String nombreDe(Estudiante e) {
        return e.getPersona().getNombre() + " " + e.getPersona().getApellido();
    }

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
