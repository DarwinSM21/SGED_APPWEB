package org.uteq.backend.deportivo.asistencia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.service.NotificacionService;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.dto.AsistenciaDtos.AsistenciaResponse;
import org.uteq.backend.deportivo.asistencia.dto.AsistenciaDtos.DiaAsistenciaResponse;
import org.uteq.backend.deportivo.asistencia.dto.AsistenciaDtos.MapaAsistenciaResponse;
import org.uteq.backend.deportivo.asistencia.dto.AsistenciaDtos.MiHistorialResponse;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registra la asistencia del ESTUDIANTE autenticado. El token QR ya se
 * canjeo en el controller (AsistenciaQrController.marcar) antes de llamar
 * aqui; este servicio solo resuelve quien es el estudiante, decide
 * PRESENTE vs TARDE, y persiste.
 */
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final EstudianteRepository estudianteRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final NotificacionService notificacionService;

    @Value("${asistencia.tolerancia-tarde-minutos:10}")
    private int toleranciaTardeMinutos;

    @Transactional
    public Asistencia marcarPorQr(String username, Long idSesion) {
        Estudiante estudiante = estudianteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un estudiante asociado a esta cuenta"));

        asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(idSesion, estudiante.getIdEstudiante())
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya marcaste tu asistencia en esta sesión");
                });

        SesionEntrenamiento sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada con id: " + idSesion));

        Boolean categoriaCoincide = asistenciaRepository.validarCategoriaCoincide(
                estudiante.getIdEstudiante(), idSesion);
        if (categoriaCoincide == null || !categoriaCoincide) {
            throw new IllegalArgumentException("Esta sesión no corresponde a tu categoría");
        }

        LocalTime ahora = LocalTime.now(Zonas.ECUADOR);
        Asistencia asistencia = Asistencia.builder()
                .sesion(sesion)
                .estudiante(estudiante)
                .horaEntrada(ahora)
                .metodo(Asistencia.METODO_QR)
                .estado(calcularEstado(sesion.getHoraInicio(), ahora))
                .build();

        asistencia = asistenciaRepository.save(asistencia);
        notificacionService.notificarAsistencia(estudiante, asistencia.getEstado());
        return asistencia;
    }

    /** Sin hora_inicio programada no hay contra que medir la tardanza: PRESENTE. */
    private String calcularEstado(LocalTime horaInicio, LocalTime ahora) {
        if (horaInicio == null) {
            return Asistencia.ESTADO_PRESENTE;
        }
        LocalTime limite = horaInicio.plusMinutes(toleranciaTardeMinutos);
        return ahora.isAfter(limite) ? Asistencia.ESTADO_TARDE : Asistencia.ESTADO_PRESENTE;
    }

    /**
     * Historial propio del ESTUDIANTE autenticado. Antes solo podia marcar
     * asistencia, no consultar lo que ya habia marcado.
     */
    @Transactional(readOnly = true)
    public MiHistorialResponse misAsistencias(String username) {
        Estudiante estudiante = estudianteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un estudiante asociado a esta cuenta"));

        List<AsistenciaResponse> asistencias = asistenciaRepository
                .findByEstudiante_IdEstudianteOrderBySesion_FechaDesc(estudiante.getIdEstudiante(), Pageable.unpaged())
                .getContent().stream()
                .map(this::aResponse)
                .toList();

        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        BigDecimal porcentaje = asistenciaRepository
                .calcularPorcentajeAsistencia(estudiante.getIdEstudiante(), hoy.minusDays(30), hoy);

        return new MiHistorialResponse(asistencias, porcentaje);
    }

    private AsistenciaResponse aResponse(Asistencia a) {
        return new AsistenciaResponse(
                a.getIdAsistencia(),
                a.getSesion().getFecha(),
                a.getSesion().getCategoria().getNombre(),
                a.getHoraEntrada(),
                a.getEstado());
    }

    /**
     * Mapa de asistencia de los ultimos {@code dias} dias. Solo devuelve los
     * dias que tuvieron entrenamiento: un sabado sin sesion no es un dia de
     * asistencia cero, es un dia que no cuenta, y pintarlo igual que un
     * martes al que no fue nadie seria mentir con el color.
     *
     * El corte es ayer por el mismo motivo que el reporte por estudiante: la
     * sesion de hoy puede no haber ocurrido todavia.
     */
    @Transactional(readOnly = true)
    public MapaAsistenciaResponse mapaDeAsistencia(int dias) {
        int ventana = Math.max(7, Math.min(dias, 120));
        LocalDate hasta = LocalDate.now(Zonas.ECUADOR).minusDays(1);
        LocalDate desde = hasta.minusDays(ventana - 1L);

        // Una fila por dia y categoria; si dos categorias entrenaron el mismo
        // dia se acumulan, porque el mapa mide el dia completo.
        Map<LocalDate, long[]> porDia = new LinkedHashMap<>();
        for (Object[] fila : sesionRepository.resumenAsistenciaPorDia(desde, hasta)) {
            LocalDate fecha = (LocalDate) fila[0];
            long presentes = fila[1] == null ? 0L : ((Number) fila[1]).longValue();
            long esperados = fila[2] == null ? 0L : ((Number) fila[2]).longValue();
            long[] acumulado = porDia.computeIfAbsent(fecha, f -> new long[2]);
            acumulado[0] += presentes;
            acumulado[1] += esperados;
        }

        List<DiaAsistenciaResponse> diasConEntrenamiento = new ArrayList<>(porDia.size());
        porDia.forEach((fecha, acumulado) -> diasConEntrenamiento.add(new DiaAsistenciaResponse(
                fecha, acumulado[0], acumulado[1], porcentaje(acumulado[0], acumulado[1]))));
        diasConEntrenamiento.sort(Comparator.comparing(DiaAsistenciaResponse::fecha));

        List<DiaAsistenciaResponse> medibles = diasConEntrenamiento.stream()
                .filter(d -> d.esperados() > 0)
                .toList();

        BigDecimal promedio = medibles.isEmpty() ? BigDecimal.ZERO
                : medibles.stream()
                        .map(DiaAsistenciaResponse::porcentaje)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(medibles.size()), 2, RoundingMode.HALF_UP);

        return new MapaAsistenciaResponse(
                desde, hasta, diasConEntrenamiento, promedio,
                medibles.stream().max(Comparator.comparing(DiaAsistenciaResponse::porcentaje)).orElse(null),
                medibles.stream().min(Comparator.comparing(DiaAsistenciaResponse::porcentaje)).orElse(null));
    }

    private BigDecimal porcentaje(long presentes, long esperados) {
        if (esperados <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(presentes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(esperados), 2, RoundingMode.HALF_UP);
    }
}
