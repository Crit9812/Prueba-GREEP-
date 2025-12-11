package Compartido.exportar;

import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class exportador {

    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // -----------------------
    // Método público para exportar según tipo seleccionado
    // -----------------------
    public static <T> void exportarTabla(TableView<T> tabla, String titulo, String tipo) {
        if (tabla.getItems().isEmpty()) {
            mostrarError("No hay datos para exportar.");
            return;
        }

        // Selección de carpeta en lugar de archivo
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar carpeta para guardar el archivo");
        File carpeta = dirChooser.showDialog(tabla.getScene().getWindow());

        if (carpeta != null) {
            String fechaHora = LocalDateTime.now().format(FILE_FORMATTER);
            String extension = tipo.equalsIgnoreCase("pdf") ? ".pdf" : ".xlsx";
            File archivo = new File(carpeta, titulo + "-" + fechaHora + extension);

            try {
                if (tipo.equalsIgnoreCase("pdf")) {
                    exportarPDF(tabla, titulo, archivo);
                } else {
                    exportarExcel(tabla, titulo, archivo);
                }
                mostrarExito("Archivo generado correctamente:\n" + archivo.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error al exportar: " + e.getMessage());
            }
        }
    }

    // -----------------------
    // Exportar PDF
    // -----------------------
    private static <T> void exportarPDF(TableView<T> tabla, String titulo, File archivo) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font fontTitleBold = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font fontHeaderBold = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            PDType1Font fontFooter = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

            float margin = 50;
            float rowHeight = 20;
            float yStart;
            float tableWidth;

            int pageNumber = 1;
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            yStart = page.getMediaBox().getHeight() - margin;
            tableWidth = page.getMediaBox().getWidth() - 2 * margin;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Título
            float yPosition = yStart;
            contentStream.beginText();
            contentStream.setFont(fontTitleBold, 16);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(titulo);
            contentStream.endText();
            yPosition -= 30;

            // Encabezados
            List<TableColumn<T, ?>> columns = tabla.getColumns();
            List<String> headers = columns.stream().map(TableColumn::getText).toList();

            int numColumns = headers.size();
            float colWidth = tableWidth / numColumns;

            contentStream.setFont(fontHeaderBold, 10);
            float textx = margin;
            for (String header : headers) {
                contentStream.beginText();
                contentStream.newLineAtOffset(textx + 2, yPosition - 15);
                contentStream.showText(header);
                contentStream.endText();
                textx += colWidth;
            }

            yPosition -= rowHeight;
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(margin + tableWidth, yPosition);
            contentStream.stroke();
            yPosition -= 10;
            contentStream.setFont(fontNormal, 9);

            for (T item : tabla.getItems()) {
                if (yPosition <= margin + 50) {
                    contentStream.close();
                    agregarPieDePagina(document, page, margin, tableWidth, fontFooter, pageNumber);
                    pageNumber++;

                    page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = yStart - 50;

                    contentStream.setFont(fontHeaderBold, 10);
                    textx = margin;
                    for (String header : headers) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(textx + 2, yPosition - 15);
                        contentStream.showText(header);
                        contentStream.endText();
                        textx += colWidth;
                    }

                    yPosition -= rowHeight;
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(margin + tableWidth, yPosition);
                    contentStream.stroke();
                    yPosition -= 10;
                    contentStream.setFont(fontNormal, 9);
                }

                textx = margin;
                for (TableColumn<T, ?> col : columns) {
                    Object value = col.getCellData(item);
                    String text = value != null ? value.toString() : "";

                    contentStream.beginText();
                    contentStream.newLineAtOffset(textx + 2, yPosition - 15);
                    contentStream.showText(truncateText(text, 20));
                    contentStream.endText();

                    textx += colWidth;
                }

                yPosition -= rowHeight;
            }

            contentStream.close();
            agregarPieDePagina(document, page, margin, tableWidth, fontFooter, pageNumber);

            document.save(archivo);
        }
    }

    private static void agregarPieDePagina(PDDocument document, PDPage page, float margin, float tableWidth, PDType1Font fontFooter, int pageNumber) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {

            String fechaHora = LocalDateTime.now().format(DATE_FORMATTER);
            String pieIzquierdo = "Página " + pageNumber;
            String pieDerecho = "Generado: " + fechaHora;

            contentStream.beginText();
            contentStream.setFont(fontFooter, 8);
            contentStream.newLineAtOffset(margin, margin - 10);
            contentStream.showText(pieIzquierdo);
            contentStream.endText();

            float textoAncho = fontFooter.getStringWidth(pieDerecho) / 1000 * 8;
            contentStream.beginText();
            contentStream.setFont(fontFooter, 8);
            contentStream.newLineAtOffset(margin + tableWidth - textoAncho, margin - 10);
            contentStream.showText(pieDerecho);
            contentStream.endText();
        }
    }

    // -----------------------
    // Exportar Excel
    // -----------------------
    private static <T> void exportarExcel(TableView<T> tabla, String titulo, File archivo) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(titulo);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm:ss"));

        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue(titulo);
        Cell dateCell = titleRow.createCell(tabla.getColumns().size());
        dateCell.setCellValue(LocalDateTime.now());
        dateCell.setCellStyle(dateStyle);

        List<TableColumn<T, ?>> columns = tabla.getColumns();
        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns.get(i).getText());
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 2;
        for (T item : tabla.getItems()) {
            Row row = sheet.createRow(rowNum++);
            int colIndex = 0;
            for (TableColumn<T, ?> col : columns) {
                Object value = col.getCellData(item);
                row.createCell(colIndex++).setCellValue(value != null ? value.toString() : "");
            }
        }

        for (int i = 0; i < columns.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(archivo)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) return text.substring(0, maxLength - 3) + "...";
        return text;
    }

    private static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
