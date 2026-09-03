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
import org.uteq.backend.deportivo.asistencia.dto.PasarListaDtos.FilaNomina;
import org.uteq.backend.deportivo.asistencia.dto.PasarListaDtos.MarcaAsistencia;
import org.uteq.backend.deportivo.asistencia.dto.PasarListaDtos.NominaResponse;
import org.uteq.backend.deportivo.asistencia.dto.PasarListaDtos.PasarListaRequest;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

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
 * Asistencia a sesiones de entrenamiento por dos vías: el QR que marca el
 * propio estudiante (mejor dato, con hora real) y la lista manual del
 * entrenador. La lista manual es la última palabra sobre quién estuvo en la
 * cancha y no inventa hora de llegada: hora presente ⇒ la midió el QR, hora
 * vacía ⇒ es palabra del entrenador.
 */
@Service
@RequiredArgsConstructor
public class AsistenciaService {
    private final AsistenciaRepository asistenciaRepository;
    private final EstudianteRepository estudianteRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final NotificacionService notificacionService;

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
     * @throws RecursoNoEncontradoException si la cuenta no tiene estudiante
     *                                      asociado o la sesión no existe
     * @throws IllegalArgumentException     si ya marcó asistencia en esa
     *                                      sesión o la sesión no es de su
     *                                      categoría
     */
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

        LocalTime ahora = LocalTime.now(Zonas.ECUADOR).truncatedTo(ChronoUnit.SECONDS);
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

    /**
     * Nómina de la sesión: <em>todos</em> los estudiantes activos de la
     * categoría, con lo que ya esté registrado de cada uno. Se parte de la
     * categoría y no de la tabla de asistencias porque el entrenador necesita
     * ver a quién le falta marcar.
     *
     * @param idSesion identificador de la sesión
     * @return la nómina, con el indicador de si aún es editable
     * @throws RecursoNoEncontradoException si la sesión no existe
     */
    @Transactional(readOnly = true)
    public NominaResponse nomina(Long idSesion) {
        SesionEntrenamiento sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada con id: " + idSesion));

        Map<Long, Asistencia> yaRegistradas = new LinkedHashMap<>();
        for (Asistencia a : asistenciaRepository.findBySesionIdSesion(idSesion)) {
            yaRegistradas.put(a.getEstudiante().getIdEstudiante(), a);
        }

        List<FilaNomina> filas = new ArrayList<>();
        for (Estudiante e : estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(
                        sesion.getCategoria().getIdCategoria())) {
            Asistencia a = yaRegistradas.get(e.getIdEstudiante());
            filas.add(new FilaNomina(
                    e.getIdEstudiante(),
                    e.getPersona().getNombre() + " " + e.getPersona().getApellido(),
                    a == null ? null : a.getEstado(),
                    a == null ? null : a.getMetodo(),
                    a == null ? null : a.getHoraEntrada(),
                    a == null ? null : a.getObservacion()));
        }

        String motivo = motivoNoEditable(sesion);
        return new NominaResponse(idSesion, sesion.getCategoria().getNombre(), sesion.getFecha(),
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
     * @throws RecursoNoEncontradoException si la sesión o algún estudiante no
     *                                      existen
     * @throws IllegalArgumentException     si la sesión aún no ocurrió o
     *                                      algún estudiante no es de la
     *                                      categoría de la sesión
     */
    @Transactional
    public NominaResponse pasarLista(Long idSesion, PasarListaRequest request) {
        SesionEntrenamiento sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada con id: " + idSesion));

        String motivo = motivoNoEditable(sesion);
        if (motivo != null) {
            throw new IllegalArgumentException(motivo);
        }

        Map<Long, Asistencia> existentes = new LinkedHashMap<>();
        for (Asistencia a : asistenciaRepository.findBySesionIdSesion(idSesion)) {
            existentes.put(a.getEstudiante().getIdEstudiante(), a);
        }

        for (MarcaAsistencia marca : request.marcas()) {
            Estudiante estudiante = estudianteRepository
                    .findByIdEstudianteAndActivoTrue(marca.idEstudiante())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Estudiante no encontrado o inactivo: " + marca.idEstudiante()));

            // Un estudiante de otra categoría ensuciaría el porcentaje de
            // asistencia de ambas. Se resuelve en Java (no con el procedimiento
            // que usa el QR): pasar lista valida a los veinticinco de una
            // categoría, y el procedimiento sería un N+1.
            if (!estudiante.getCategoria().getIdCategoria()
                    .equals(sesion.getCategoria().getIdCategoria())) {
                throw new IllegalArgumentException(estudiante.getPersona().getNombre() + " "
                        + estudiante.getPersona().getApellido() + " no pertenece a "
                        + sesion.getCategoria().getNombre());
            }

            boolean estuvo = Asistencia.ESTADO_PRESENTE.equals(marca.estado())
                    || Asistencia.ESTADO_TARDE.equals(marca.estado());

            Asistencia a = existentes.get(marca.idEstudiante());
            if (a == null) {
                a = Asistencia.builder().sesion(sesion).estudiante(estudiante).build();
            }
            a.setEstado(marca.estado());
            a.setObservacion(marca.observacion());

            // La lista manual NO inventa una hora de llegada: el entrenador
            // afirma que el chico estuvo, no a qué hora entró.
            if (!estuvo) {
                a.setHoraEntrada(null);
                a.setMetodo(Asistencia.METODO_MANUAL);
            } else if (a.getHoraEntrada() == null) {
                a.setMetodo(Asistencia.METODO_MANUAL);
            }
            asistenciaRepository.save(a);
        }

        return nomina(idSesion);
    }

    // Una sesión que todavía no ocurrió no admite lista: nadie pudo asistir.
    private String motivoNoEditable(SesionEntrenamiento sesion) {
        if (sesion.getFecha().isAfter(LocalDate.now(Zonas.ECUADOR))) {
            return "La sesión es del " + sesion.getFecha() + ": todavía no ocurre";
        }
        return null;
    }

    // Sin hora_inicio programada no hay contra qué medir la tardanza: PRESENTE.
    private String calcularEstado(LocalTime horaInicio, LocalTime ahora) {
        if (horaInicio == null) {
            return Asistencia.ESTADO_PRESENTE;
        }
        LocalTime limite = horaInicio.plusMinutes(toleranciaTardeMinutos);
        return ahora.isAfter(limite) ? Asistencia.ESTADO_TARDE : Asistencia.ESTADO_PRESENTE;
    }

    /**
     * Historial de asistencia del estudiante autenticado, con su porcentaje
     * de los últimos 30 días.
     *
     * @param username nombre de usuario del estudiante
     * @return el historial y el porcentaje reciente
     * @throws RecursoNoEncontradoException si la cuenta no tiene estudiante
     *                                      asociado
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
     * Mapa de asistencia de los últimos {@code dias} días. Solo devuelve los
     * días que tuvieron entrenamiento; el corte es ayer, porque la sesión de
     * hoy puede no haber ocurrido todavía.
     *
     * @param dias ventana solicitada; se acota a {@code [7, 120]}
     * @return la serie diaria, el promedio y los días mejor y peor
     */
    @Transactional(readOnly = true)
    public MapaAsistenciaResponse mapaDeAsistencia(int dias) {
        int ventana = Math.max(7, Math.min(dias, 120));
        LocalDate hasta = LocalDate.now(Zonas.ECUADOR).minusDays(1);
        LocalDate desde = hasta.minusDays(ventana - 1L);

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
