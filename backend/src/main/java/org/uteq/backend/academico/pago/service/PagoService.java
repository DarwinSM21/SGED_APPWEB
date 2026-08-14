package org.uteq.backend.academico.pago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.academico.pago.dto.PagoDtos.IngresosMesResponse;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

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

    @Transactional
    public List<Pago> registrarMembresia(Long idEstudiante, int anio, List<Integer> meses,
                                          BigDecimal monto, LocalDate fechaPago, String usernameRegistrador) {
        Estudiante estudiante = buscarEstudiante(idEstudiante);
        Usuario registrador = buscarUsuario(usernameRegistrador);

        List<Integer> mesesUnicos = meses.stream().distinct().sorted().toList();
        for (Integer mes : mesesUnicos) {
            if (pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMes(
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
        long cantidad = pagoRepository.countByFechaPagoBetween(inicio, fin);
        return new IngresosMesResponse(mesActual.getYear(), mesActual.getMonthValue(), total, cantidad);
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
