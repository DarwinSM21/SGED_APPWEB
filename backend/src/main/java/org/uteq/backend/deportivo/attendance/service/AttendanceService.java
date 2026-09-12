package org.uteq.backend.deportivo.attendance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.service.NotificationService;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.attendance.dto.AttendanceDtos.AttendanceResponse;
import org.uteq.backend.deportivo.attendance.dto.AttendanceDtos.AttendanceDayResponse;
import org.uteq.backend.deportivo.attendance.dto.AttendanceDtos.AttendanceMapResponse;
import org.uteq.backend.deportivo.attendance.dto.AttendanceDtos.MyHistoryResponse;
import org.uteq.backend.deportivo.attendance.dto.TakeAttendanceDtos.RosterRow;
import org.uteq.backend.deportivo.attendance.dto.TakeAttendanceDtos.AttendanceMark;
import org.uteq.backend.deportivo.attendance.dto.TakeAttendanceDtos.RosterResponse;
import org.uteq.backend.deportivo.attendance.dto.TakeAttendanceDtos.TakeAttendanceRequest;
import org.uteq.backend.deportivo.attendance.entity.Attendance;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.session.entity.TrainingSession;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Attendance a sesiones de entrenamiento por dos vías: el QR que marca el
 * propio estudiante (mejor dato, con hora real) y la lista manual del
 * entrenador. La lista manual es la última palabra sobre quién estuvo en la
 * cancha y no inventa hora de llegada: hora presente ⇒ la midió el QR, hora
 * vacía ⇒ es palabra del entrenador.
 */
@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository estudianteRepository;
    private final TrainingSessionRepository sesionRepository;
    private final NotificationService notificacionService;

    /** Minutos de gracia tras la hora de inicio antes de contar TARDE. */
    @Value("${asistencia.tolerancia-tarde-minutos:10}")
    private int toleranciaTardeMinutos;

    /**
     * Registra la asistencia del estudiante autenticado tras un canjeo de QR
     * ya validado en el controlador. Resuelve quién es el estudiante, decide
     * {@code PRESENTE} vs {@code TARDE}, persiste y notifica a los
     * representantes.
     *
     * @param username nombre de usuario del estudiante
     * @param idSesion sesión a la que corresponde el token canjeado
     * @return la asistencia registrada
     * @throws ResourceNotFoundException si la cuenta no tiene estudiante
     *                                      asociado o la sesión no existe
     * @throws IllegalArgumentException     si ya marcó asistencia en esa
     *                                      sesión o la sesión no es de su
     *                                      categoría
     */
    @Transactional
    public Attendance markByQr(String username, Long idSesion) {
        Student estudiante = estudianteRepository.findByUserAccount_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un estudiante asociado a esta cuenta"));

        attendanceRepository.findBySession_IdAndStudent_Id(idSesion, estudiante.getId())
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya marcaste tu asistencia en esta sesión");
                });

        TrainingSession sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada con id: " + idSesion));

        Boolean categoriaCoincide = attendanceRepository.matchesCategory(
                estudiante.getId(), idSesion);
        if (categoriaCoincide == null || !categoriaCoincide) {
            throw new IllegalArgumentException("Esta sesión no corresponde a tu categoría");
        }

        LocalTime ahora = LocalTime.now(Zones.ECUADOR).truncatedTo(ChronoUnit.SECONDS);
        Attendance asistencia = Attendance.builder()
                .sesion(sesion)
                .estudiante(estudiante)
                .horaEntrada(ahora)
                .metodo(Attendance.METODO_QR)
                .estado(calculateStatus(sesion.getHoraInicio(), ahora))
                .build();

        asistencia = attendanceRepository.save(asistencia);
        notificacionService.notifyAttendance(estudiante, asistencia.getEstado());
        return asistencia;
    }

    /**
     * Nómina de la sesión: <em>todos</em> los estudiantes activos de la
     * categoría, con lo que ya esté registrado de cada uno. Se parte de la
     * categoría y no de la tabla de asistencias porque el entrenador necesita
     * ver a quién le falta marcar.
     *
     * @param idSesion identificador de la sesión
     * @return la nómina, con el indicador de si aún es editable
     * @throws ResourceNotFoundException si la sesión no existe
     */
    @Transactional(readOnly = true)
    public RosterResponse roster(Long idSesion) {
        TrainingSession sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada con id: " + idSesion));

        Map<Long, Attendance> yaRegistradas = new LinkedHashMap<>();
        for (Attendance a : attendanceRepository.findBySession_Id(idSesion)) {
            yaRegistradas.put(a.getEstudiante().getId(), a);
        }

        List<RosterRow> filas = new ArrayList<>();
        for (Student e : estudianteRepository
                .findByCategory_IdCategoriaAndActiveTrueOrderByPerson_LastNameAsc(
                        sesion.getCategoria().getIdCategoria())) {
            Attendance a = yaRegistradas.get(e.getId());
            filas.add(new RosterRow(
                    e.getId(),
                    e.getPerson().getName() + " " + e.getPerson().getLastName(),
                    a == null ? null : a.getEstado(),
                    a == null ? null : a.getMetodo(),
                    a == null ? null : a.getHoraEntrada(),
                    a == null ? null : a.getObservacion()));
        }

        String motivo = notEditableReason(sesion);
        return new RosterResponse(idSesion, sesion.getCategoria().getNombre(), sesion.getFecha(),
                sesion.getHoraInicio(), motivo == null, motivo, filas);
    }

    /**
     * Lista manual del entrenador. Es un <em>upsert</em> por
     * {@code (sesión, estudiante)}: se puede volver a pasar lista para
     * corregir, y sobrescribe incluso lo que vino por QR.
     *
     * @param idSesion identificador de la sesión
     * @param request  estado y observación por estudiante
     * @return la nómina resultante
     * @throws ResourceNotFoundException si la sesión o algún estudiante no
     *                                      existen
     * @throws IllegalArgumentException     si la sesión aún no ocurrió o
     *                                      algún estudiante no es de la
     *                                      categoría de la sesión
     */
    @Transactional
    public RosterResponse takeAttendance(Long idSesion, TakeAttendanceRequest request) {
        TrainingSession sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada con id: " + idSesion));

        String motivo = notEditableReason(sesion);
        if (motivo != null) {
            throw new IllegalArgumentException(motivo);
        }

        Map<Long, Attendance> existentes = new LinkedHashMap<>();
        for (Attendance a : attendanceRepository.findBySession_Id(idSesion)) {
            existentes.put(a.getEstudiante().getId(), a);
        }

        for (AttendanceMark marca : request.marcas()) {
            Student estudiante = estudianteRepository
                    .findByIdAndActiveTrue(marca.idEstudiante())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estudiante no encontrado o inactivo: " + marca.idEstudiante()));

            // Un estudiante de otra categoría ensuciaría el porcentaje de
            // asistencia de ambas. Se resuelve en Java (no con el procedimiento
            // que usa el QR): pasar lista valida a los veinticinco de una
            // categoría, y el procedimiento sería un N+1.
            if (!estudiante.getCategory().getIdCategoria()
                    .equals(sesion.getCategoria().getIdCategoria())) {
                throw new IllegalArgumentException(estudiante.getPerson().getName() + " "
                        + estudiante.getPerson().getLastName() + " no pertenece a "
                        + sesion.getCategoria().getNombre());
            }

            boolean estuvo = Attendance.ESTADO_PRESENTE.equals(marca.estado())
                    || Attendance.ESTADO_TARDE.equals(marca.estado());

            Attendance a = existentes.get(marca.idEstudiante());
            if (a == null) {
                a = Attendance.builder().sesion(sesion).estudiante(estudiante).build();
            }
            a.setEstado(marca.estado());
            a.setObservacion(marca.observacion());

            // La lista manual NO inventa una hora de llegada: el entrenador
            // afirma que el chico estuvo, no a qué hora entró.
            if (!estuvo) {
                a.setHoraEntrada(null);
                a.setMetodo(Attendance.METODO_MANUAL);
            } else if (a.getHoraEntrada() == null) {
                a.setMetodo(Attendance.METODO_MANUAL);
            }
            attendanceRepository.save(a);
        }

        return roster(idSesion);
    }

    // Una sesión que todavía no ocurrió no admite lista: nadie pudo asistir.
    private String notEditableReason(TrainingSession sesion) {
        if (sesion.getFecha().isAfter(LocalDate.now(Zones.ECUADOR))) {
            return "La sesión es del " + sesion.getFecha() + ": todavía no ocurre";
        }
        return null;
    }

    // Sin hora_inicio programada no hay contra qué medir la tardanza: PRESENTE.
    private String calculateStatus(LocalTime horaInicio, LocalTime ahora) {
        if (horaInicio == null) {
            return Attendance.ESTADO_PRESENTE;
        }
        LocalTime limite = horaInicio.plusMinutes(toleranciaTardeMinutos);
        return ahora.isAfter(limite) ? Attendance.ESTADO_TARDE : Attendance.ESTADO_PRESENTE;
    }

    /**
     * Historial de asistencia del estudiante autenticado, con su porcentaje
     * de los últimos 30 días.
     *
     * @param username nombre de usuario del estudiante
     * @return el historial y el porcentaje reciente
     * @throws ResourceNotFoundException si la cuenta no tiene estudiante
     *                                      asociado
     */
    @Transactional(readOnly = true)
    public MyHistoryResponse myAttendances(String username) {
        Student estudiante = estudianteRepository.findByUserAccount_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un estudiante asociado a esta cuenta"));

        List<AttendanceResponse> asistencias = attendanceRepository
                .findByStudent_IdOrderBySession_DateDesc(estudiante.getId(), Pageable.unpaged())
                .getContent().stream()
                .map(this::toResponse)
                .toList();

        LocalDate hoy = LocalDate.now(Zones.ECUADOR);
        BigDecimal porcentaje = attendanceRepository
                .calculateAttendancePercentage(estudiante.getId(), hoy.minusDays(30), hoy);

        return new MyHistoryResponse(asistencias, porcentaje);
    }

    private AttendanceResponse toResponse(Attendance a) {
        return new AttendanceResponse(
                a.getIdAsistencia(),
                a.getSesion().getFecha(),
                a.getSesion().getCategoria().getNombre(),
                a.getHoraEntrada(),
                a.getEstado());
    }

    /**
     * Mapa de asistencia de los últimos {@code dias} días. Solo devuelve los
     * días que tuvieron entrenamiento; el corte es ayer, porque la sesión de
     * hoy puede no haber ocurrido todavía.
     *
     * @param dias ventana solicitada; se acota a {@code [7, 120]}
     * @return la serie diaria, el promedio y los días mejor y peor
     */
    @Transactional(readOnly = true)
    public AttendanceMapResponse attendanceMap(int dias) {
        int ventana = Math.max(7, Math.min(dias, 120));
        LocalDate hasta = LocalDate.now(Zones.ECUADOR).minusDays(1);
        LocalDate desde = hasta.minusDays(ventana - 1L);

        Map<LocalDate, long[]> porDia = new LinkedHashMap<>();
        for (Object[] fila : sesionRepository.attendanceSummaryByDay(desde, hasta)) {
            LocalDate fecha = (LocalDate) fila[0];
            long presentes = fila[1] == null ? 0L : ((Number) fila[1]).longValue();
            long esperados = fila[2] == null ? 0L : ((Number) fila[2]).longValue();
            long[] acumulado = porDia.computeIfAbsent(fecha, f -> new long[2]);
            acumulado[0] += presentes;
            acumulado[1] += esperados;
        }

        List<AttendanceDayResponse> diasConEntrenamiento = new ArrayList<>(porDia.size());
        porDia.forEach((fecha, acumulado) -> diasConEntrenamiento.add(new AttendanceDayResponse(
                fecha, acumulado[0], acumulado[1], percentage(acumulado[0], acumulado[1]))));
        diasConEntrenamiento.sort(Comparator.comparing(AttendanceDayResponse::fecha));

        List<AttendanceDayResponse> medibles = diasConEntrenamiento.stream()
                .filter(d -> d.esperados() > 0)
                .toList();

        BigDecimal promedio = medibles.isEmpty() ? BigDecimal.ZERO
                : medibles.stream()
                        .map(AttendanceDayResponse::porcentaje)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(medibles.size()), 2, RoundingMode.HALF_UP);

        return new AttendanceMapResponse(
                desde, hasta, diasConEntrenamiento, promedio,
                medibles.stream().max(Comparator.comparing(AttendanceDayResponse::porcentaje)).orElse(null),
                medibles.stream().min(Comparator.comparing(AttendanceDayResponse::porcentaje)).orElse(null));
    }

    private BigDecimal percentage(long presentes, long esperados) {
        if (esperados <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(presentes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(esperados), 2, RoundingMode.HALF_UP);
    }
}
