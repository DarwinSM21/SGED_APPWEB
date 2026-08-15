package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uteq.backend.reportes.service.ReportePdfService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportePdfServiceTest {

    private final ReportePdfService servicio = new ReportePdfService();

    @Test
    @DisplayName("genera un PDF valido con filas de datos")
    void generaPdfConFilas() {
        byte[] pdf = servicio.generar("Reporte de prueba",
                List.of("Nombre", "Valor"),
                List.of(List.of("Ana Torres", "100"), List.of("Juan Pérez", "200")));

        assertTrue(pdf.length > 0);
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
    }

    @Test
    @DisplayName("genera un PDF valido aunque no haya filas (mensaje de 'sin datos')")
    void generaPdfSinFilas() {
        byte[] pdf = servicio.generar("Reporte vacío", List.of("Nombre"), List.of());

        assertTrue(pdf.length > 0);
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
    }
}
