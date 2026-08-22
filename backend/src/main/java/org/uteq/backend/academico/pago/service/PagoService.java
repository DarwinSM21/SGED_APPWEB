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
 * Registro de pagos. MEMBRESIA valida, antes de guardar nada, que ningun
 * mes solicitado este ya cubierto (todo o nada: si uno falla no se cobra
 * a medias). DIARIO no tiene esa validacion porque no cubre periodo.
 */
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;

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

    @Transactional(readOnly = true)
    public List<Pago> historialDe(Long idEstudiante) {
        if (!estudianteRepository.existsById(idEstudiante)) {
            throw new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante);
        }
        return pagoRepository.findByEstudiante_IdEstudianteOrderByFechaPagoDesc(idEstudiante);
    }

    /** Cuanto entro en caja este mes calendario (Ecuador), sin importar que mes cubre cada pago. */
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
     * Serie de recaudacion de los ultimos {@code meses} meses, contando el
     * actual. Se arma sobre la lista completa de meses del rango y no sobre
     * lo que devuelve la base: un mes sin cobros tiene que viajar en cero,
     * porque si se omite el grafico dibuja contiguos dos meses que no lo son
     * y la tendencia que se lee es falsa.
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

        // El promedio se divide entre todos los meses del rango, incluidos
        // los que no tuvieron cobros: dividir solo entre los meses con
        // movimiento inflaria el numero justo cuando peor va la cobranza.
        BigDecimal promedio = total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);

        IngresosMesResponse mejor = serie.stream()
                .filter(m -> m.total().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(IngresosMesResponse::total))
                .orElse(null);

        return new HistoricoIngresosResponse(serie, total, promedio, mejor);
    }

    /**
     * Anula un pago mal registrado. No lo edita ni lo borra: el registro se
     * queda con quien lo anulo, cuando y por que, y el correcto se registra
     * aparte. Asi el historial cuenta lo que de verdad paso -hubo un error y
     * se corrigio- en vez de esconderlo, que es lo que hace falta cuando
     * alguien revisa las cuentas meses despues.
     *
     * Anular dos veces se rechaza en vez de ignorarse en silencio: si alguien
     * lo intenta es que cree estar anulando algo vigente, y conviene decirle
     * que no lo esta.
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
