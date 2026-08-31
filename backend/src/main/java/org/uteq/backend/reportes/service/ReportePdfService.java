package org.uteq.backend.reportes.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.uteq.backend.common.Zonas;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportePdfService {
    private static final Font FUENTE_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font FUENTE_ENCABEZADO = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FUENTE_CELDA = new Font(Font.HELVETICA, 9);
    private static final Font FUENTE_PIE = new Font(Font.HELVETICA, 8, Font.ITALIC);
    private static final Color COLOR_ENCABEZADO = new Color(79, 70, 229);

    public byte[] generar(String titulo, List<String> encabezados, List<List<String>> filas) {
        Document documento = new Document(PageSize.A4, 36, 36, 54, 54);
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph("SGED - " + titulo, FUENTE_TITULO));
            documento.add(new Paragraph(" "));

            if (filas.isEmpty()) {
                documento.add(new Paragraph("No hay datos para los filtros seleccionados.", FUENTE_CELDA));
            } else {
                documento.add(construirTabla(encabezados, filas));
            }

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(
                    "Generado por " + usuarioActual() + " el " + fechaActual(), FUENTE_PIE));

            documento.close();
            return salida.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + titulo, e);
        }
    }

    private PdfPTable construirTabla(List<String> encabezados, List<List<String>> filas) {
        PdfPTable tabla = new PdfPTable(encabezados.size());
        tabla.setWidthPercentage(100);

        for (String encabezado : encabezados) {
            PdfPCell celda = new PdfPCell(new Phrase(encabezado, FUENTE_ENCABEZADO));
            celda.setBackgroundColor(COLOR_ENCABEZADO);
            celda.setPadding(5);
            tabla.addCell(celda);
        }
        for (List<String> fila : filas) {
            for (String valor : fila) {
                PdfPCell celda = new PdfPCell(new Phrase(valor != null ? valor : "-", FUENTE_CELDA));
                celda.setPadding(4);
                tabla.addCell(celda);
            }
        }
        return tabla;
    }

    private String usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "desconocido";
    }

    private String fechaActual() {
        return LocalDateTime.now(Zonas.ECUADOR).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
