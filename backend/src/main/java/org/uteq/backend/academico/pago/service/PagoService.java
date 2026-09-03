package org.uteq.backend.academico.pago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.academico.pago.dto.PagoDtos.HistoricoIngresosResponse;
import org.uteq.backend.academico.pago.dto.PagoDtos.IngresosMesResponse;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de pagos. {@code MEMBRESIA} valida, antes de guardar nada, que
 * ningún mes solicitado esté ya cubierto (todo o nada: si uno falla no se
 * cobra a medias). {@code DIARIO} no tiene esa validación porque no cubre
 * período. Las anulaciones no borran ni editan: dejan el pago con quién,
 * cuándo y por qué se anuló, y el correcto se registra aparte.
 */
@Service
@RequiredArgsConstructor
public class PagoService {
    private final PagoRepository pagoRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra el pago de una o varias mensualidades. Es todo o nada: si
     * cualquiera de los meses ya está cubierto, no se guarda ninguno.
     *
     * @param idEstudiante        estudiante que paga
     * @param anio                año de las mensualidades
     * @param meses               meses a cubrir (1–12); se ordenan y
     *                            deduplican
     * @param monto               monto por mes
     * @param fechaPago           fecha del cobro; {@code null} usa la de hoy
     * @param usernameRegistrador usuario que registra el pago
     * @return los pagos creados, uno por mes
     * @throws RecursoNoEncontradoException si el estudiante no existe
     * @throws IllegalArgumentException     si algún mes ya está cubierto
     */
    @Auditado(accion = "CREAR", entidad = "Pago",
            descripcionSpel = "'creó ' + #result.size() + ' pago(s) de membresía (estudiante #' + #p0 + ')'")
    @Transactional
    public List<Pago> registrarMembresia(Long idEstudiante, int anio, List<Integer> meses,
                                          BigDecimal monto, LocalDate fechaPago, String usernameRegistrador) {
        Estudiante estudiante = buscarEstudiante(idEstudiante);
        Usuario registrador = buscarUsuario(usernameRegistrador);

        List<Integer> mesesUnicos = meses.stream().distinct().sorted().toList();
        for (Integer mes : mesesUnicos) {
            if (pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                    idEstudiante, TipoPago.MEMBRESIA, (short) anio, mes.shortValue())) {
                throw new IllegalArgumentException(
                        "El mes " + mes + "/" + anio + " ya está cubierto para este estudiante");
            }
        }

        LocalDate fecha = fechaPago != null ? fechaPago : LocalDate.now(Zonas.ECUADOR);
        List<Pago> pagos = mesesUnicos.stream()
                .map(mes -> Pago.builder()
                        .estudiante(estudiante)
                        .tipo(TipoPago.MEMBRESIA)
                        .anio((short) anio)
                        .mes(mes.shortValue())
                        .monto(monto)
                        .fechaPago(fecha)
                        .registradoPor(registrador)
                        .build())
                .toList();
        return pagoRepository.saveAll(pagos);
    }

    /**
     * Registra un pago diario (no cubre período, sin validación de duplicado).
     *
     * @param idEstudiante        estudiante que paga
     * @param monto               monto pagado
     * @param fechaPago           fecha del cobro; {@code null} usa la de hoy
     * @param usernameRegistrador usuario que registra el pago
     * @return el pago creado
     * @throws RecursoNoEncontradoException si el estudiante no existe
     */
    @Auditado(accion = "CREAR", entidad = "Pago", idSpel = "#result.idPago",
            descripcionSpel = "'registró un pago diario de $' + #p1 + ' (estudiante #' + #p0 + ')'")
    @Transactional
    public Pago registrarDiario(Long idEstudiante, BigDecimal monto, LocalDate fechaPago, String usernameRegistrador) {
        Estudiante estudiante = buscarEstudiante(idEstudiante);
        Usuario registrador = buscarUsuario(usernameRegistrador);

        return pagoRepository.save(Pago.builder()
                .estudiante(estudiante)
                .tipo(TipoPago.DIARIO)
                .monto(monto)
                .fechaPago(fechaPago != null ? fechaPago : LocalDate.now(Zonas.ECUADOR))
                .registradoPor(registrador)
                .build());
    }

    /**
     * Historial de pagos de un estudiante, del más reciente al más antiguo.
     *
     * @param idEstudiante identificador del estudiante
     * @return la lista de pagos (incluye los anulados)
     * @throws RecursoNoEncontradoException si el estudiante no existe
     */
    @Transactional(readOnly = true)
    public List<Pago> historialDe(Long idEstudiante) {
        if (!estudianteRepository.existsById(idEstudiante)) {
            throw new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante);
        }
        return pagoRepository.findByEstudiante_IdEstudianteOrderByFechaPagoDesc(idEstudiante);
    }

    /**
     * Cuánto entró en caja este mes calendario (Ecuador), sin importar qué
     * mes cubre cada pago.
     *
     * @return año, mes, total cobrado y número de pagos vigentes del mes
     */
    @Transactional(readOnly = true)
    public IngresosMesResponse ingresosDelMes() {
        YearMonth mesActual = YearMonth.now(Zonas.ECUADOR);
        LocalDate inicio = mesActual.atDay(1);
        LocalDate fin = mesActual.atEndOfMonth();

        BigDecimal total = pagoRepository.sumarMontoEntreFechas(inicio, fin);
        long cantidad = pagoRepository.countByFechaPagoBetweenAndAnuladoEnIsNull(inicio, fin);
        return new IngresosMesResponse(mesActual.getYear(), mesActual.getMonthValue(), total, cantidad);
    }

    /**
     * Serie de recaudación de los últimos {@code meses} meses, contando el
     * actual. Se arma sobre la lista completa de meses del rango: un mes sin
     * cobros viaja en cero, porque si se omite el gráfico dibuja contiguos
     * dos meses que no lo son. El promedio se divide entre todos los meses
     * del rango, incluidos los de cero.
     *
     * @param meses número de meses solicitado; se acota a {@code [1, 24]}
     * @return la serie mensual, el total, el promedio y el mejor mes
     *         ({@code null} si no hubo cobros)
     */
    @Transactional(readOnly = true)
    public HistoricoIngresosResponse historicoIngresos(int meses) {
        int cantidad = Math.max(1, Math.min(meses, 24));
        YearMonth actual = YearMonth.now(Zonas.ECUADOR);
        YearMonth primero = actual.minusMonths(cantidad - 1L);

        Map<YearMonth, IngresosMesResponse> porMes = new HashMap<>();
        for (Object[] fila : pagoRepository.totalesPorMesDeCobro(primero.atDay(1), actual.atEndOfMonth())) {
            int anio = ((Number) fila[0]).intValue();
            int mes = ((Number) fila[1]).intValue();
            porMes.put(YearMonth.of(anio, mes), new IngresosMesResponse(
                    anio, mes, (BigDecimal) fila[2], ((Number) fila[3]).longValue()));
        }

        List<IngresosMesResponse> serie = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            YearMonth m = primero.plusMonths(i);
            serie.add(porMes.getOrDefault(m, new IngresosMesResponse(
                    m.getYear(), m.getMonthValue(), BigDecimal.ZERO, 0L)));
        }

        BigDecimal total = serie.stream()
                .map(IngresosMesResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal promedio = total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);

        IngresosMesResponse mejor = serie.stream()
                .filter(m -> m.total().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(IngresosMesResponse::total))
                .orElse(null);

        return new HistoricoIngresosResponse(serie, total, promedio, mejor);
    }

    /**
     * Anula un pago mal registrado. No lo edita ni lo borra: el registro se
     * queda con quién lo anuló, cuándo y por qué. Anular dos veces se rechaza
     * en vez de ignorarse en silencio.
     *
     * @param idPago          identificador del pago
     * @param motivo          motivo de la anulación
     * @param usernameAnulador usuario que anula
     * @return el pago anulado
     * @throws RecursoNoEncontradoException si el pago no existe
     * @throws IllegalArgumentException     si el pago ya estaba anulado
     */
    @Auditado(accion = "ANULAR", entidad = "Pago", idSpel = "#p0")
    @Transactional
    public Pago anular(Long idPago, String motivo, String usernameAnulador) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado con id: " + idPago));

        if (!pago.estaVigente()) {
            throw new IllegalArgumentException("Este pago ya estaba anulado");
        }

        pago.setAnuladoEn(java.time.OffsetDateTime.now());
        pago.setAnuladoPor(buscarUsuario(usernameAnulador));
        pago.setMotivoAnulacion(motivo);
        return pagoRepository.save(pago);
    }

    private Estudiante buscarEstudiante(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + id));
    }

    private Usuario buscarUsuario(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + username));
    }
}
