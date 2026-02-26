package Reportes.historial.exportar;

import Reportes.historial.model.MovimientoFactura;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReporteMovimientoExporter {

    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static void exportar(MovimientoFactura movimiento, Window parentWindow) {
        if (movimiento == null) return;

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar carpeta para guardar el reporte");
        File carpeta = dirChooser.showDialog(parentWindow);
        if (carpeta == null) return;

        String nombreArchivo = "movimiento_" + movimiento.getClaveMovimiento() + "_"
                + LocalDateTime.now().format(FILE_FORMATTER) + ".pdf";
        File archivo = new File(carpeta, nombreArchivo);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font fuenteTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fuenteTexto = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float y = page.getMediaBox().getHeight() - 60;

                content.setNonStrokingColor(0.5686f, 0.8314f, 0.5216f);
                content.addRect(35, y - 35, page.getMediaBox().getWidth() - 70, 35);
                content.fill();

                escribir(content, fuenteTitulo, 18, 45, y - 24, "Reporte de movimiento");
                y -= 65;

                escribir(content, fuenteTitulo, 12, 40, y, "Resumen");
                y -= 22;

                y = linea(content, fuenteTexto, 11, y, "Clave del movimiento", movimiento.getClaveMovimiento());
                y = linea(content, fuenteTexto, 11, y, "Fecha", movimiento.getFecha());
                y = linea(content, fuenteTexto, 11, y, "Hora", movimiento.getHora());
                y = linea(content, fuenteTexto, 11, y, "Tipo de movimiento", movimiento.getTipoMovimiento());
                y = linea(content, fuenteTexto, 11, y, "Total", movimiento.getTotal());
                y = linea(content, fuenteTexto, 11, y, "Usuario", movimiento.getUsuario());
                y = linea(content, fuenteTexto, 11, y, "Externo", movimiento.getExterno());
                y = linea(content, fuenteTexto, 11, y, "Factura", movimiento.getFacturaExterna());
                y = linea(content, fuenteTexto, 11, y, "Estado", movimiento.getEstado());
            }

            document.save(archivo);
            mostrarExito("✅ Reporte generado correctamente\n\n" + archivo.getAbsolutePath());
        } catch (IOException e) {
            mostrarError("❌ Error al generar el reporte:\n" + e.getMessage());
        }
    }

    private static float linea(PDPageContentStream content, PDType1Font font, int size,
                               float y, String label, String value) throws IOException {
        escribir(content, font, size, 50, y, label + ": " + (value == null ? "-" : value));
        return y - 18;
    }

    private static void escribir(PDPageContentStream content, PDType1Font font, int size,
                                 float x, float y, String text) throws IOException {
        content.beginText();
        content.setNonStrokingColor(0, 0, 0);
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al generar reporte");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reporte generado");
        alert.setHeaderText("Reporte generado");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
