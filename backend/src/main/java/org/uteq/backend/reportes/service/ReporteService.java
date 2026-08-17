package org.uteq.backend.reportes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.entity.DetalleEvaluacion;
import org.uteq.backend.deportivo.evaluacion.entity.EvaluacionEstudiante;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Arma las filas de cada reporte reutilizando los repositorios de negocio
 * ya existentes (sin duplicar logica de consulta); ReportePdfService solo
 * se encarga del formato del PDF.
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportePdfService pdfService;
    private final EstudianteRepository estudianteRepository;
    private final PagoRepository pagoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final LesionRepository lesionRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;

    @Transactional(readOnly = true)
    public byte[] estudiantesFichas(Long idCategoria, Boolean activo) {
        var encontrados = sinVacio(estudianteRepository.buscarParaReporte(idCategoria, activo));
        var filas = encontrados.stream()
                .map(e -> List.of(
                        e.getCodigoEstudiante(),
                        e.getPersona().getNombre() + " " + e.getPersona().getApellido(),
                        e.getCategoria().getNombre(),
                        Boolean.TRUE.equals(e.getActivo()) ? "Activo" : "Inactivo",
                        e.getFechaIngreso().format(FECHA)))
                .toList();
        return pdfService.generar("Reporte de Fichas de Estudiantes",
                List.of("Código", "Estudiante", "Categoría", "Estado", "Fecha ingreso"), filas);
    }

    @Transactional(readOnly = true)
    public byte[] pagos(Long idEstudiante, LocalDate desde, LocalDate hasta) {
        Specification<Pago> spec = Specification.<Pago>where(igualA("estudiante.idEstudiante", idEstudiante))
                .and(this.<Pago>desdeDe("fechaPago", desde))
                .and(this.<Pago>hastaDe("fechaPago", hasta));
        var filas = sinVacio(pagoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaPago"))).stream()
                .map(this::filaPago)
                .toList();
        return pdfService.generar("Reporte de Pagos",
                List.of("Estudiante", "Tipo", "Período", "Monto", "Fecha de pago", "Registrado por"), filas);
    }

    @Transactional(readOnly = true)
    public byte[] asistencias(Long idEstudiante, Long idCategoria, LocalDate desde, LocalDate hasta) {
        Specification<Asistencia> spec = Specification.<Asistencia>where(igualA("estudiante.idEstudiante", idEstudiante))
                .and(this.<Asistencia>igualA("estudiante.categoria.idCategoria", idCategoria))
                .and(this.<Asistencia>desdeDe("sesion.fecha", desde))
                .and(this.<Asistencia>hastaDe("sesion.fecha", hasta));
        var filas = sinVacio(asistenciaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "sesion.fecha"))).stream()
                .map(this::filaAsistencia)
                .toList();
        return pdfService.generar("Reporte de Asistencias",
                List.of("Estudiante", "Categoría", "Fecha sesión", "Estado", "Método"), filas);
    }

    @Transactional(readOnly = true)
    public byte[] evaluaciones(Long idEstudiante, Long idCategoria, LocalDate desde, LocalDate hasta) {
        Specification<EvaluacionEstudiante> spec = Specification.<EvaluacionEstudiante>where(igualA("estudiante.idEstudiante", idEstudiante))
                .and(this.<EvaluacionEstudiante>igualA("categoriaDia.idCategoria", idCategoria))
                .and(this.<EvaluacionEstudiante>desdeDe("evaluacion.fecha", desde))
                .and(this.<EvaluacionEstudiante>hastaDe("evaluacion.fecha", hasta));
        var filas = sinVacio(evaluacionEstudianteRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "evaluacion.fecha"))).stream()
                .map(this::filaEvaluacion)
                .toList();
        return pdfService.generar("Reporte de Evaluaciones",
                List.of("Estudiante", "Categoría", "Fecha", "Posición", "Promedio"), filas);
    }

    @Transactional(readOnly = true)
    public byte[] lesiones(Long idEstudiante, Long idCategoria, LocalDate desde, LocalDate hasta) {
        Specification<Lesion> spec = Specification.<Lesion>where(igualA("estudiante.idEstudiante", idEstudiante))
                .and(this.<Lesion>igualA("estudiante.categoria.idCategoria", idCategoria))
                .and(this.<Lesion>desdeDe("fechaLesion", desde))
                .and(this.<Lesion>hastaDe("fechaLesion", hasta));
        var filas = sinVacio(lesionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaLesion"))).stream()
                .map(this::filaLesion)
                .toList();
        return pdfService.generar("Reporte de Lesiones",
                List.of("Estudiante", "Descripción", "Fecha lesión", "Retorno estimado", "Estado"), filas);
    }

    /**
     * Predicados de filtro opcional para los reportes, construidos con Criteria API en vez
     * de "(:x IS NULL OR campo = :x)" en JPQL: ese patron dispara "could not determine data
     * type of parameter" en Postgres cuando el parametro solo aparece en un IS NULL sin otro
     * contexto tipado (Hibernate 6 + pgjdbc). Con Specification, un filtro ausente simplemente
     * no agrega predicado -- nunca se envia un parametro ambiguo.
     */
    private <T> Specification<T> igualA(String ruta, Object valor) {
        if (valor == null) return Specification.<T>where(null);
        return (root, query, cb) -> cb.equal(this.<T, Object>ruta(root, ruta), valor);
    }

    private <T> Specification<T> desdeDe(String ruta, LocalDate desde) {
        if (desde == null) return Specification.<T>where(null);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(this.<T, LocalDate>ruta(root, ruta), desde);
    }

    private <T> Specification<T> hastaDe(String ruta, LocalDate hasta) {
        if (hasta == null) return Specification.<T>where(null);
        return (root, query, cb) -> cb.lessThanOrEqualTo(this.<T, LocalDate>ruta(root, ruta), hasta);
    }

    @SuppressWarnings("unchecked")
    private <T, Y> jakarta.persistence.criteria.Path<Y> ruta(
            jakarta.persistence.criteria.Root<T> root, String puntos) {
        jakarta.persistence.criteria.Path<Object> path = null;
        for (String segmento : puntos.split("\\.")) {
            path = path == null ? root.get(segmento) : path.get(segmento);
        }
        return (jakarta.persistence.criteria.Path<Y>) (jakarta.persistence.criteria.Path<?>) path;
    }

    private <T> List<T> sinVacio(List<T> resultados) {
        if (resultados.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay datos para los filtros seleccionados");
        }
        return resultados;
    }

    private List<String> filaPago(Pago p) {
        String periodo = p.getTipo() == Pago.TipoPago.MEMBRESIA ? p.getMes() + "/" + p.getAnio() : "-";
        var registrador = p.getRegistradoPor().getPersona();
        return List.of(
                p.getEstudiante().getPersona().getNombre() + " " + p.getEstudiante().getPersona().getApellido(),
                p.getTipo().name(),
                periodo,
                p.getMonto().toPlainString(),
                p.getFechaPago().format(FECHA),
                registrador.getNombre() + " " + registrador.getApellido());
    }

    private List<String> filaAsistencia(Asistencia a) {
        return List.of(
                a.getEstudiante().getPersona().getNombre() + " " + a.getEstudiante().getPersona().getApellido(),
                a.getEstudiante().getCategoria().getNombre(),
                a.getSesion().getFecha().format(FECHA),
                a.getEstado(),
                a.getMetodo());
    }

    private List<String> filaEvaluacion(EvaluacionEstudiante ee) {
        String posicion = ee.getPosicionJugada() != null ? ee.getPosicionJugada().getNombre() : "-";
        String promedio = ee.getDetalles().isEmpty() ? "-" : promedio(ee.getDetalles());
        return List.of(
                ee.getEstudiante().getPersona().getNombre() + " " + ee.getEstudiante().getPersona().getApellido(),
                ee.getCategoriaDia().getNombre(),
                ee.getEvaluacion().getFecha().format(FECHA),
                posicion,
                promedio);
    }

    private List<String> filaLesion(Lesion l) {
        String retorno = l.getFechaEstimadaRetorno() != null ? l.getFechaEstimadaRetorno().format(FECHA) : "-";
        String estado = l.estaActiva() ? "Activa" : "De alta el " + l.getFechaAlta().format(FECHA);
        return List.of(
                l.getEstudiante().getPersona().getNombre() + " " + l.getEstudiante().getPersona().getApellido(),
                l.getDescripcion(),
                l.getFechaLesion().format(FECHA),
                retorno,
                estado);
    }

    private String promedio(List<DetalleEvaluacion> detalles) {
        BigDecimal suma = detalles.stream().map(DetalleEvaluacion::getPuntaje).reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(detalles.size()), 2, RoundingMode.HALF_UP).toPlainString();
    }
}
