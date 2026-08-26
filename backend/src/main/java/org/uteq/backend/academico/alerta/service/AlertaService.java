package org.uteq.backend.academico.alerta.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.alerta.dto.AlertaDtos.EstudianteEnRiesgoResponse;
import org.uteq.backend.academico.alerta.dto.AlertaDtos.PanelAlertasResponse;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Panel operativo: que estudiantes necesitan atencion hoy y por que.
 *
 * <p>Cruza tres señales que hasta ahora vivian separadas en tres pantallas
 * distintas -pagos, evaluacion diaria y lesiones-, de modo que quien abre
 * el sistema por la mañana ve en un solo lugar a quien hay que llamar. Se
 * devuelven las tres por separado en vez de un unico indicador de riesgo:
 * la accion no es la misma si el problema es la cuota, las faltas o una
 * lesion.
 *
 * <p>Las tres señales se resuelven con una consulta cada una, nunca con una
 * por estudiante. La de asistencia lo era hasta que se midio: llamar a
 * sp_reporte_asistencia_estudiante en bucle costaba 0,16 s con 8 alumnos
 * pero 2,9 s con 2.008. El procedimiento sigue siendo lo correcto para
 * consultar a UN estudiante -ficha e informe al representante lo usan-; lo
 * que no escala es invocarlo tantas veces como alumnos haya.
 *
 * <p>Se conserva la misma regla del procedimiento: el denominador son las
 * sesiones programadas de su categoria -no solo las filas de asistencia que
 * existan- y la ventana se corta en ayer, porque una sesion de hoy puede no
 * haber ocurrido todavia y contarla seria acusar de faltar a quien aun no
 * tenia como asistir.
 */
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final EstudianteRepository estudianteRepository;
    private final PagoRepository pagoRepository;
    private final LesionRepository lesionRepository;
    private final AsistenciaRepository asistenciaRepository;

    /** Por debajo de este porcentaje la asistencia se considera un problema. */
    @Value("${alertas.umbral-asistencia:75}")
    private int umbralAsistencia;

    /** Ventana sobre la que se mide la asistencia, en dias. */
    @Value("${alertas.dias-asistencia:30}")
    private int diasAsistencia;

    /**
     * Cuantos estudiantes se detallan. El panel es una lista de a quien
     * llamar hoy, no un censo: con 3.000 alumnos activos la respuesta pasaba
     * de medio mega y el navegador pintaba 2.995 filas -180.000 px de alto,
     * 265 pantallas de scroll-, que no se lee ni se usa. Los CONTADORES se
     * siguen calculando sobre la lista completa, asi que el recorte no
     * miente sobre cuantos hay: solo deja de dibujarlos.
     */
    @Value("${alertas.tope-detalle:25}")
    private int topeDetalle;

    @Transactional(readOnly = true)
    public PanelAlertasResponse panel() {
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        short anio = (short) hoy.getYear();
        short mes = (short) hoy.getMonthValue();

        List<Estudiante> activos = estudianteRepository.findByActivoTrueOrderByPersona_ApellidoAsc();

        // Las dos primeras señales salen de una consulta cada una, no de una
        // por estudiante: preguntar de a uno serian tantas consultas como
        // alumnos tenga la escuela.
        Set<Long> alDia = new HashSet<>(
                pagoRepository.idsConMembresiaCubierta(TipoPago.MEMBRESIA, anio, mes));
        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());

        // Se corta en ayer por el mismo motivo que el procedimiento: la sesion
        // de hoy puede no haber ocurrido y contarla castigaria a todos.
        LocalDate corte = hoy.minusDays(1);
        LocalDate desde = hoy.minusDays(diasAsistencia);
        BigDecimal umbral = BigDecimal.valueOf(umbralAsistencia);

        Map<Long, BigDecimal> porcentajes = porcentajesPorEstudiante(desde, corte);

        List<EstudianteEnRiesgoResponse> enRiesgo = activos.stream()
                .map(e -> evaluar(e, alDia, lesionados, porcentajes, umbral))
                .filter(r -> r.totalAlertas() > 0)
                .sorted(Comparator
                        .comparingInt(EstudianteEnRiesgoResponse::totalAlertas).reversed()
                        .thenComparing(EstudianteEnRiesgoResponse::nombreCompleto))
                .toList();

        // El orden ya puso primero a los que acumulan mas señales, asi que
        // recortar por arriba se queda con los mas urgentes y no con los
        // primeros alfabeticamente.
        List<EstudianteEnRiesgoResponse> detalle = enRiesgo.size() > topeDetalle
                ? enRiesgo.subList(0, topeDetalle)
                : enRiesgo;

        return new PanelAlertasResponse(
                anio, mes, umbralAsistencia, activos.size(),
                enRiesgo.stream().filter(EstudianteEnRiesgoResponse::mensualidadPendiente).count(),
                enRiesgo.stream().filter(EstudianteEnRiesgoResponse::asistenciaBaja).count(),
                enRiesgo.stream().filter(EstudianteEnRiesgoResponse::lesionActiva).count(),
                enRiesgo.size(),
                detalle);
    }

    /**
     * Porcentaje por estudiante a partir de una sola consulta. Un estudiante
     * SIN entrada aqui -o con cero sesiones programadas- se deja fuera del
     * mapa a proposito: el servicio lo leera como null, que significa "sin
     * dato", no "cero por ciento". La diferencia importa porque marcar
     * asistencia baja a quien no tuvo entrenamientos seria acusarlo de algo
     * que no hizo.
     */
    private Map<Long, BigDecimal> porcentajesPorEstudiante(LocalDate desde, LocalDate corte) {
        Map<Long, BigDecimal> porcentajes = new HashMap<>();
        for (Object[] fila : asistenciaRepository.resumenAsistenciaDeActivos(desde, corte)) {
            long programadas = ((Number) fila[1]).longValue();
            if (programadas == 0) continue;
            long presentes = ((Number) fila[2]).longValue();
            porcentajes.put(
                    ((Number) fila[0]).longValue(),
                    BigDecimal.valueOf(presentes)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(programadas), 2, RoundingMode.HALF_UP));
        }
        return porcentajes;
    }

    private EstudianteEnRiesgoResponse evaluar(
            Estudiante e, Set<Long> alDia, Set<Long> lesionados,
            Map<Long, BigDecimal> porcentajes, BigDecimal umbral) {

        Long id = e.getIdEstudiante();
        boolean debe = !alDia.contains(id);
        boolean lesionada = lesionados.contains(id);

        BigDecimal porcentaje = porcentajes.get(id);
        // Null significa "su categoria no tuvo sesiones programadas en el
        // rango": eso no es asistencia baja, es ausencia de datos, y marcarlo
        // como alerta acusaria al estudiante de algo que no hizo.
        boolean asistenciaBaja = porcentaje != null && porcentaje.compareTo(umbral) < 0;

        int total = (debe ? 1 : 0) + (asistenciaBaja ? 1 : 0) + (lesionada ? 1 : 0);
        var persona = e.getPersona();

        return new EstudianteEnRiesgoResponse(
                id,
                persona == null ? "(sin persona)" : persona.getNombre() + " " + persona.getApellido(),
                e.getCategoria() == null ? null : e.getCategoria().getNombre(),
                debe, asistenciaBaja, porcentaje, lesionada, total);
    }
}
