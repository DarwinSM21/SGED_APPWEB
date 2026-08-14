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
import org.uteq.backend.deportivo.asistencia.dto.AsistenciaDtos.MiHistorialResponse;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
}
