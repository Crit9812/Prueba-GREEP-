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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class exportador {

    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // -----------------------
    // Metodo público para exportar según tipo seleccionado
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

        final int MAX_COLUMNAS_POR_SECCION = 6;

        try (PDDocument document = new PDDocument()) {

            PDType1Font fontTitleBold = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font fontHeaderBold = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            PDType1Font fontFooter = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

            float margin = 50;
            float rowHeight = 20;

            int pageNumber = 1;

            // ✅ OMITIR SIEMPRE LA PRIMERA COLUMNA
            List<TableColumn<T, ?>> allColumns = tabla.getColumns().size() > 1
                    ? tabla.getColumns().subList(1, tabla.getColumns().size())
                    : List.of();

            List<T> dataItems = tabla.getItems();

            float pageWidth = PDRectangle.A4.getHeight();
            float pageHeight = PDRectangle.A4.getWidth();
            float tableWidth = pageWidth - (2 * margin);

            // -----------------------
            // DIVIDIR COLUMNAS EN SECCIONES
            // -----------------------
            List<List<TableColumn<T, ?>>> columnSections = new ArrayList<>();

            for (int i = 0; i < allColumns.size(); i += MAX_COLUMNAS_POR_SECCION) {
                columnSections.add(
                        allColumns.subList(
                                i,
                                Math.min(i + MAX_COLUMNAS_POR_SECCION, allColumns.size())
                        )
                );
            }

            // -----------------------
            // CALCULAR FILAS POR PÁGINA
            // -----------------------
            float usableHeight = pageHeight - (margin * 2) - 60; // título + encabezados
            int rowsPerPage = (int) (usableHeight / rowHeight);

            // -----------------------
            // PAGINACIÓN: FILAS → COLUMNAS
            // -----------------------
            for (int rowStart = 0; rowStart < dataItems.size(); rowStart += rowsPerPage) {

                int rowEnd = Math.min(rowStart + rowsPerPage, dataItems.size());
                List<T> rowBlock = dataItems.subList(rowStart, rowEnd);

                for (int sectionIndex = 0; sectionIndex < columnSections.size(); sectionIndex++) {

                    List<TableColumn<T, ?>> section = columnSections.get(sectionIndex);

                    PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                    document.addPage(page);

                    float yStart = pageHeight - margin;
                    float yPosition = yStart;

                    PDPageContentStream contentStream = new PDPageContentStream(document, page);

                    // -----------------------
                    // TÍTULO
                    // -----------------------
                    contentStream.beginText();
                    contentStream.setFont(fontTitleBold, 16);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(
                            titulo +
                                    " | Filas " + (rowStart + 1) + "-" + rowEnd +
                                    " | Columnas " +
                                    (sectionIndex * MAX_COLUMNAS_POR_SECCION + 1) +
                                    "-" +
                                    (sectionIndex * MAX_COLUMNAS_POR_SECCION + section.size())
                    );
                    contentStream.endText();

                    yPosition -= 40;

                    float colWidth = tableWidth / section.size();

                    // -----------------------
                    // ENCABEZADOS
                    // -----------------------
                    contentStream.setFont(fontHeaderBold, 10);
                    float textx = margin;

                    for (TableColumn<T, ?> col : section) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(textx + 2, yPosition - 15);
                        contentStream.showText(
                                truncateTextToWidth(col.getText(), colWidth, fontHeaderBold, 10)
                        );
                        contentStream.endText();
                        textx += colWidth;
                    }

                    yPosition -= rowHeight;
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(margin + tableWidth, yPosition);
                    contentStream.stroke();

                    yPosition -= 10;
                    contentStream.setFont(fontNormal, 9);

                    // -----------------------
                    // FILAS
                    // -----------------------
                    for (T item : rowBlock) {

                        textx = margin;

                        for (TableColumn<T, ?> col : section) {

                            Object value = col.getCellData(item);
                            String text = value != null ? value.toString() : "";

                            contentStream.beginText();
                            contentStream.newLineAtOffset(textx + 2, yPosition - 15);
                            contentStream.showText(
                                    truncateTextToWidth(text, colWidth, fontNormal, 9)
                            );
                            contentStream.endText();

                            textx += colWidth;
                        }

                        yPosition -= rowHeight;
                    }

                    contentStream.close();
                    agregarPieDePagina(document, page, margin, tableWidth, fontFooter, pageNumber++);
                }
            }

            document.save(archivo);
        }
    }

    private static String truncateTextToWidth(
            String text,
            float maxWidth,
            PDType1Font font,
            float fontSize) throws IOException {

        if (text == null || text.isEmpty()) return "";

        String result = text;

        while (font.getStringWidth(result) / 1000 * fontSize > maxWidth - 4) {
            if (result.length() <= 3) {
                return "...";
            }
            result = result.substring(0, result.length() - 1);
        }

        if (!result.equals(text)) {
            result += "...";
        }

        return result;
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
