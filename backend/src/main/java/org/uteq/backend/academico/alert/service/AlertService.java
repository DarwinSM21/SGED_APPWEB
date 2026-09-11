package org.uteq.backend.academico.alert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.alert.dto.AlertDtos.StudentAtRiskResponse;
import org.uteq.backend.academico.alert.dto.AlertDtos.AlertsPanelResponse;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.payment.entity.Payment.PaymentType;
import org.uteq.backend.academico.payment.repository.PaymentRepository;
import org.uteq.backend.common.Zones;
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
 * Panel operativo: qué estudiantes necesitan atención hoy y por qué.
 *
 * <p>Cruza tres señales que vivían separadas en tres pantallas —pagos,
 * evaluación diaria y lesiones— para que quien abre el sistema por la mañana
 * vea en un solo lugar a quién hay que llamar. Se devuelven por separado,
 * no como un único indicador de riesgo: la acción no es la misma si el
 * problema es la cuota, las faltas o una lesión.
 *
 * <p>Cada señal se resuelve con una consulta, nunca con una por estudiante.
 * La regla de asistencia es la del procedimiento almacenado: el denominador
 * son las sesiones programadas de la categoría y la ventana se corta en
 * ayer, porque una sesión de hoy puede no haber ocurrido todavía.
 */
@Service
@RequiredArgsConstructor
public class AlertService {
    private final StudentRepository estudianteRepository;
    private final PaymentRepository pagoRepository;
    private final LesionRepository lesionRepository;
    private final AsistenciaRepository asistenciaRepository;

    /** Por debajo de este porcentaje la asistencia se considera un problema. */
    @Value("${alertas.umbral-asistencia:75}")
    private int umbralAsistencia;

    /** Ventana sobre la que se mide la asistencia, en días. */
    @Value("${alertas.dias-asistencia:30}")
    private int diasAsistencia;

    // Cuántos estudiantes se detallan. El panel es una lista de a quién llamar
    // hoy, no un censo: los contadores se siguen calculando sobre la lista
    // completa, así que el recorte no miente sobre cuántos hay.
    @Value("${alertas.tope-detalle:25}")
    private int topeDetalle;

    /**
     * Construye el panel de alertas del día: número de estudiantes activos,
     * contadores por tipo de alerta (mensualidad, asistencia, lesión) y el
     * detalle recortado de los más urgentes.
     *
     * @return el panel de alertas
     */
    @Transactional(readOnly = true)
    public AlertsPanelResponse panel() {
        LocalDate hoy = LocalDate.now(Zones.ECUADOR);
        short anio = (short) hoy.getYear();
        short mes = (short) hoy.getMonthValue();

        List<Student> activos = estudianteRepository.findByActiveTrueOrderByPerson_LastNameAsc();

        Set<Long> alDia = new HashSet<>(
                pagoRepository.idsWithMembershipCovered(PaymentType.MEMBRESIA, anio, mes));
        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());

        LocalDate corte = hoy.minusDays(1);
        LocalDate desde = hoy.minusDays(diasAsistencia);
        BigDecimal umbral = BigDecimal.valueOf(umbralAsistencia);

        Map<Long, BigDecimal> porcentajes = percentagesByStudent(desde, corte);

        List<StudentAtRiskResponse> enRiesgo = activos.stream()
                .map(e -> evaluate(e, alDia, lesionados, porcentajes, umbral))
                .filter(r -> r.totalAlertas() > 0)
                .sorted(Comparator
                        .comparingInt(StudentAtRiskResponse::totalAlertas).reversed()
                        .thenComparing(StudentAtRiskResponse::nombreCompleto))
                .toList();

        List<StudentAtRiskResponse> detalle = enRiesgo.size() > topeDetalle
                ? enRiesgo.subList(0, topeDetalle)
                : enRiesgo;

        return new AlertsPanelResponse(
                anio, mes, umbralAsistencia, activos.size(),
                enRiesgo.stream().filter(StudentAtRiskResponse::mensualidadPendiente).count(),
                enRiesgo.stream().filter(StudentAtRiskResponse::asistenciaBaja).count(),
                enRiesgo.stream().filter(StudentAtRiskResponse::lesionActiva).count(),
                enRiesgo.size(),
                detalle);
    }

    // Porcentaje por estudiante a partir de una sola consulta. Un estudiante
    // SIN entrada aquí —o con cero sesiones programadas— se deja fuera del
    // mapa a propósito: el servicio lo lee como null ("sin dato", no "cero por
    // ciento"). Marcar asistencia baja a quien no tuvo entrenamientos sería
    // acusarlo de algo que no hizo.
    private Map<Long, BigDecimal> percentagesByStudent(LocalDate desde, LocalDate corte) {
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

    private StudentAtRiskResponse evaluate(
            Student e, Set<Long> alDia, Set<Long> lesionados,
            Map<Long, BigDecimal> porcentajes, BigDecimal umbral) {
        Long id = e.getId();
        boolean debe = !alDia.contains(id);
        boolean lesionada = lesionados.contains(id);

        BigDecimal porcentaje = porcentajes.get(id);

        boolean asistenciaBaja = porcentaje != null && porcentaje.compareTo(umbral) < 0;

        int total = (debe ? 1 : 0) + (asistenciaBaja ? 1 : 0) + (lesionada ? 1 : 0);
        var persona = e.getPerson();

        return new StudentAtRiskResponse(
                id,
                persona == null ? "(sin persona)" : persona.getName() + " " + persona.getLastName(),
                e.getCategory() == null ? null : e.getCategory().getNombre(),
                debe, asistenciaBaja, porcentaje, lesionada, total);
    }
}
