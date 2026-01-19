package Compartido.exportar;

import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoEntrada.model.model;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReporteTraspasoExporter {

    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void exportarReporte(String claveEntrada,
                                       List<model.DetalleEntrada> detalles,
                                       Map<String, List<UbicacionCompra>> ubicacionesPorProducto,
                                       Window owner) {
        if (detalles == null || detalles.isEmpty()) {
            mostrarError("No hay detalles para exportar.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar carpeta para guardar el reporte");
        File carpeta = owner != null ? dirChooser.showDialog(owner) : dirChooser.showDialog(null);

        if (carpeta == null) {
            return;
        }

        String fechaHora = LocalDateTime.now().format(FILE_FORMATTER);
        File archivo = new File(carpeta, "Traspaso-" + claveEntrada + "-" + fechaHora + ".pdf");

        try {
            generarReporte(archivo, claveEntrada, detalles, ubicacionesPorProducto);
            mostrarExito("Reporte generado correctamente:\n" + archivo.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al generar el reporte: " + e.getMessage());
        }
    }

    private static void generarReporte(File archivo,
                                       String claveEntrada,
                                       List<model.DetalleEntrada> detalles,
                                       Map<String, List<UbicacionCompra>> ubicacionesPorProducto) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font fontTitle = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font fontSection = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font fontText = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

            PDRectangle pageSize = PDRectangle.LETTER;
            float margin = 50f;
            float yStart = pageSize.getHeight() - margin;
            float yPosition = yStart;
            float width = pageSize.getWidth() - 2 * margin;

            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            yPosition = escribirLinea(contentStream, fontTitle, 16, margin, yPosition,
                    "Reporte de traspaso");
            yPosition -= 10;
            yPosition = escribirLinea(contentStream, fontText, 11, margin, yPosition,
                    "Clave de entrada: " + claveEntrada);
            yPosition = escribirLinea(contentStream, fontText, 11, margin, yPosition,
                    "Fecha y hora: " + LocalDateTime.now().format(DATE_FORMATTER));

            yPosition -= 15;
            yPosition = escribirLinea(contentStream, fontSection, 12, margin, yPosition,
                    "Detalles y ubicaciones");
            yPosition -= 5;

            for (model.DetalleEntrada detalle : detalles) {
                if (yPosition < margin + 120) {
                    contentStream.close();
                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = yStart;
                }

                yPosition = escribirTextoAjustado(contentStream, fontSection, 11, margin, yPosition,
                        detalle.getProducto(), width);
                yPosition = escribirLinea(contentStream, fontText, 10, margin + 10, yPosition,
                        "Clave: " + detalle.getClaveProducto());
                yPosition = escribirLinea(contentStream, fontText, 10, margin + 10, yPosition,
                        "Cantidad: " + detalle.getCantidad());
                yPosition = escribirLinea(contentStream, fontText, 10, margin + 10, yPosition,
                        "Precio unitario: " + detalle.getPrecioUnitario());
                yPosition = escribirLinea(contentStream, fontText, 10, margin + 10, yPosition,
                        "Precio total: " + detalle.getPrecioTotal());

                List<UbicacionCompra> ubicaciones = ubicacionesPorProducto != null
                        ? ubicacionesPorProducto.get(detalle.getClaveProducto())
                        : null;
                yPosition = escribirLinea(contentStream, fontText, 10, margin + 10, yPosition,
                        "Ubicaciones:");

                if (ubicaciones == null || ubicaciones.isEmpty()) {
                    yPosition = escribirLinea(contentStream, fontText, 10, margin + 20, yPosition,
                            "- Sin ubicaciones registradas");
                } else {
                    for (UbicacionCompra ubicacion : ubicaciones) {
                        String linea = "- " + ubicacion.getUbicacion() + " : " + ubicacion.getCantidad();
                        yPosition = escribirLinea(contentStream, fontText, 10, margin + 20, yPosition, linea);
                    }
                }

                yPosition -= 8;
            }

            contentStream.close();
            document.save(archivo);
        }
    }

    private static float escribirLinea(PDPageContentStream contentStream,
                                       PDType1Font font,
                                       int fontSize,
                                       float x,
                                       float y,
                                       String texto) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(texto);
        contentStream.endText();
        return y - (fontSize + 6);
    }

    private static float escribirTextoAjustado(PDPageContentStream contentStream,
                                               PDType1Font font,
                                               int fontSize,
                                               float x,
                                               float y,
                                               String texto,
                                               float maxWidth) throws IOException {
        List<String> lineas = ajustarTexto(texto, font, fontSize, maxWidth);
        float yActual = y;
        for (String linea : lineas) {
            yActual = escribirLinea(contentStream, font, fontSize, x, yActual, linea);
        }
        return yActual;
    }

    private static List<String> ajustarTexto(String texto, PDType1Font font, int fontSize, float maxWidth)
            throws IOException {
        List<String> lineas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            lineas.add("");
            return lineas;
        }
        String[] palabras = texto.split("\\s+");
        StringBuilder lineaActual = new StringBuilder();
        for (String palabra : palabras) {
            String prueba = lineaActual.length() == 0 ? palabra : lineaActual + " " + palabra;
            float ancho = font.getStringWidth(prueba) / 1000 * fontSize;
            if (ancho > maxWidth && lineaActual.length() > 0) {
                lineas.add(lineaActual.toString());
                lineaActual = new StringBuilder(palabra);
            } else {
                lineaActual = new StringBuilder(prueba);
            }
        }
        if (!lineaActual.isEmpty()) {
            lineas.add(lineaActual.toString());
        }
        return lineas;
    }

    private static void mostrarExito(String mensaje) {
        new Alert(Alert.AlertType.INFORMATION, mensaje).showAndWait();
    }

    private static void mostrarError(String mensaje) {
        new Alert(Alert.AlertType.ERROR, mensaje).showAndWait();
    }
}
