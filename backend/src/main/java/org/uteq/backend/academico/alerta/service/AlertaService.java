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

@Service
@RequiredArgsConstructor
public class AlertaService {
    private final EstudianteRepository estudianteRepository;
    private final PagoRepository pagoRepository;
    private final LesionRepository lesionRepository;
    private final AsistenciaRepository asistenciaRepository;

    @Value("${alertas.umbral-asistencia:75}")
    private int umbralAsistencia;

    @Value("${alertas.dias-asistencia:30}")
    private int diasAsistencia;

    @Value("${alertas.tope-detalle:25}")
    private int topeDetalle;

    @Transactional(readOnly = true)
    public PanelAlertasResponse panel() {
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        short anio = (short) hoy.getYear();
        short mes = (short) hoy.getMonthValue();

        List<Estudiante> activos = estudianteRepository.findByActivoTrueOrderByPersona_ApellidoAsc();

        Set<Long> alDia = new HashSet<>(
                pagoRepository.idsConMembresiaCubierta(TipoPago.MEMBRESIA, anio, mes));
        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());

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
