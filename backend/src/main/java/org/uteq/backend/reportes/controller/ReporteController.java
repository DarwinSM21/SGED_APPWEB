package org.uteq.backend.reportes.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.reportes.service.ReporteService;

import java.time.LocalDate;

/**
 * Descarga de reportes en PDF. Cada endpoint devuelve un
 * {@code application/pdf} con {@code Content-Disposition: attachment} y
 * acepta filtros opcionales; todos los filtros ausentes traen el conjunto
 * completo (acotado internamente por {@link ReporteService}).
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    /**
     * Fichas de estudiantes.
     *
     * @param categoria categoría por la que filtrar (opcional)
     * @param activo    {@code true}/{@code false} para filtrar por estado
     *                  (opcional)
     * @return {@code 200 OK} con el PDF {@code fichas-estudiantes.pdf}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no hay datos para los filtros ({@code 404})
     */
    @GetMapping("/estudiantes-fichas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> estudiantesFichas(
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) Boolean activo) {
        return pdf("fichas-estudiantes.pdf", reporteService.estudiantesFichas(categoria, activo));
    }

    /**
     * Pagos.
     *
     * @param estudianteId estudiante por el que filtrar (opcional)
     * @param fechaDesde   límite inferior de fecha de pago (opcional)
     * @param fechaHasta   límite superior de fecha de pago (opcional)
     * @return {@code 200 OK} con el PDF {@code pagos.pdf}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no hay datos para los filtros ({@code 404})
     */
    @GetMapping("/pagos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> pagos(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta) {
        return pdf("pagos.pdf", reporteService.pagos(estudianteId, fechaDesde, fechaHasta));
    }

    /**
     * Asistencias.
     *
     * @param estudianteId estudiante por el que filtrar (opcional)
     * @param categoria    categoría por la que filtrar (opcional)
     * @param fechaDesde    límite inferior de fecha de sesión (opcional)
     * @param fechaHasta    límite superior de fecha de sesión (opcional)
     * @return {@code 200 OK} con el PDF {@code asistencias.pdf}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no hay datos para los filtros ({@code 404})
     */
    @GetMapping("/asistencias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<byte[]> asistencias(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta) {
        return pdf("asistencias.pdf", reporteService.asistencias(estudianteId, categoria, fechaDesde, fechaHasta));
    }

    /**
     * Evaluaciones.
     *
     * @param estudianteId estudiante por el que filtrar (opcional)
     * @param categoria    categoría por la que filtrar (opcional)
     * @param fechaDesde    límite inferior de fecha de evaluación (opcional)
     * @param fechaHasta    límite superior de fecha de evaluación (opcional)
     * @return {@code 200 OK} con el PDF {@code evaluaciones.pdf}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no hay datos para los filtros ({@code 404})
     */
    @GetMapping("/evaluaciones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<byte[]> evaluaciones(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta) {
        return pdf("evaluaciones.pdf", reporteService.evaluaciones(estudianteId, categoria, fechaDesde, fechaHasta));
    }

    /**
     * Lesiones.
     *
     * @param estudianteId estudiante por el que filtrar (opcional)
     * @param categoria    categoría por la que filtrar (opcional)
     * @param fechaDesde    límite inferior de fecha de lesión (opcional)
     * @param fechaHasta    límite superior de fecha de lesión (opcional)
     * @return {@code 200 OK} con el PDF {@code lesiones.pdf}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no hay datos para los filtros ({@code 404})
     */
    @GetMapping("/lesiones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<byte[]> lesiones(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta) {
        return pdf("lesiones.pdf", reporteService.lesiones(estudianteId, categoria, fechaDesde, fechaHasta));
    }

    private ResponseEntity<byte[]> pdf(String nombreArchivo, byte[] contenido) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .body(contenido);
    }
}
