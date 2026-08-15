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

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/estudiantes-fichas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> estudiantesFichas(
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) Boolean activo) {
        return pdf("fichas-estudiantes.pdf", reporteService.estudiantesFichas(categoria, activo));
    }

    @GetMapping("/pagos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> pagos(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta) {
        return pdf("pagos.pdf", reporteService.pagos(estudianteId, fechaDesde, fechaHasta));
    }

    @GetMapping("/asistencias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<byte[]> asistencias(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta) {
        return pdf("asistencias.pdf", reporteService.asistencias(estudianteId, categoria, fechaDesde, fechaHasta));
    }

    @GetMapping("/evaluaciones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<byte[]> evaluaciones(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta) {
        return pdf("evaluaciones.pdf", reporteService.evaluaciones(estudianteId, categoria, fechaDesde, fechaHasta));
    }

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
