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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
 * <p>El porcentaje de asistencia no se recalcula aqui: reutiliza el
 * procedimiento almacenado sp_reporte_asistencia_estudiante, que ya resuelve
 * el detalle fino de tomar como denominador las sesiones programadas de su
 * categoria y no solo las filas de asistencia existentes.
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

        LocalDate desde = hoy.minusDays(diasAsistencia);
        BigDecimal umbral = BigDecimal.valueOf(umbralAsistencia);

        List<EstudianteEnRiesgoResponse> enRiesgo = activos.stream()
                .map(e -> evaluar(e, alDia, lesionados, desde, hoy, umbral))
                .filter(r -> r.totalAlertas() > 0)
                .sorted(Comparator
                        .comparingInt(EstudianteEnRiesgoResponse::totalAlertas).reversed()
                        .thenComparing(EstudianteEnRiesgoResponse::nombreCompleto))
                .toList();

        return new PanelAlertasResponse(
                anio, mes, umbralAsistencia, activos.size(),
                enRiesgo.stream().filter(EstudianteEnRiesgoResponse::mensualidadPendiente).count(),
                enRiesgo.stream().filter(EstudianteEnRiesgoResponse::asistenciaBaja).count(),
                enRiesgo.stream().filter(EstudianteEnRiesgoResponse::lesionActiva).count(),
                enRiesgo);
    }

    private EstudianteEnRiesgoResponse evaluar(
            Estudiante e, Set<Long> alDia, Set<Long> lesionados,
            LocalDate desde, LocalDate hasta, BigDecimal umbral) {

        Long id = e.getIdEstudiante();
        boolean debe = !alDia.contains(id);
        boolean lesionada = lesionados.contains(id);

        BigDecimal porcentaje = asistenciaRepository.calcularPorcentajeAsistencia(id, desde, hasta);
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
