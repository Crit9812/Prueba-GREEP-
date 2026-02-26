package Compartido.exportar;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class exportador {

    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Colores (RGB convertidos de HEX)
    private static final float[] COLOR_BANDA_SUPERIOR = {0.5686f, 0.8314f, 0.5216f}; // Verde #91d485
    private static final float[] COLOR_SUBTITULOS = {0.2f, 0.2f, 0.2f}; // Gris oscuro #333
    private static final float[] COLOR_ENCABEZADO_TABLA = {0.95f, 0.95f, 0.95f};  // Gris claro
    private static final float[] COLOR_FILA_PAR = {0.98f, 0.98f, 0.98f};  // Gris muy claro
    private static final float[] COLOR_FILA_IMPAR = {1.0f, 1.0f, 1.0f};   // Blanco
    private static final float[] COLOR_FILTROS = {0.2f, 0.2f, 0.2f}; // Gris oscuro #333

    // Constantes de diseño actualizadas
    private static final float MARGIN = 35f;
    private static final float ROW_HEIGHT = 26f; // Ajustado para fuente de 13
    private static final float CELL_PADDING = 5f;
    private static final int MAX_COLUMNAS_POR_SECCION = 6;

    // Variables para control de información general
    private static boolean informacionGeneralMostrada = false;
    private static int filasPorHojaEstimado = 0;

    // -----------------------
    // Metodo público para exportar según tipo seleccionado
    // -----------------------
    public static <T> void exportarTabla(TableView<T> tabla, String titulo, String tipo) {
        exportarTabla(tabla, titulo, tipo, null);
    }

    public static <T> void exportarTabla(TableView<T> tabla, String titulo, String tipo, List<String> filtros) {
        if (tabla.getItems().isEmpty()) {
            mostrarError("No hay datos para exportar.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar carpeta para guardar el archivo");
        File carpeta = dirChooser.showDialog(tabla.getScene().getWindow());

        if (carpeta != null) {
            String fechaHora = LocalDateTime.now().format(FILE_FORMATTER);
            String extension = tipo.equalsIgnoreCase("pdf") ? ".pdf" : ".xlsx";
            File archivo = new File(carpeta, titulo + "_" + fechaHora + extension);

            try {
                if (tipo.equalsIgnoreCase("pdf")) {
                    // Reiniciar estado para nueva exportación
                    informacionGeneralMostrada = false;
                    filasPorHojaEstimado = 0;
                    exportarPDF(tabla, titulo, archivo, filtros);
                } else {
                    exportarExcel(tabla, titulo, archivo, filtros);
                }
                mostrarExito("✅ Archivo generado correctamente\n\n" +
                        "Archivo: " + titulo + "_" + fechaHora + extension + "\n" +
                        "Ubicación: " + archivo.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("❌ Error al exportar: " + e.getMessage());
            }
        }
    }

    public static <T> void previsualizarPDF(TableView<T> tabla, String titulo, List<String> filtros) {
        if (tabla.getItems().isEmpty()) {
            mostrarError("No hay datos para exportar.");
            return;
        }

        try {
            String fechaHora = LocalDateTime.now().format(FILE_FORMATTER);
            File archivo = File.createTempFile(titulo + "_preview_" + fechaHora + "_", ".pdf");
            archivo.deleteOnExit();

            // Reiniciar estado para previsualización
            informacionGeneralMostrada = false;
            filasPorHojaEstimado = 0;
            exportarPDF(tabla, titulo, archivo, filtros);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo);
            } else {
                mostrarError("No se pudo abrir la previsualización.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error al generar la previsualización: " + e.getMessage());
        }
    }

    // -----------------------
    // Exportar PDF
    // -----------------------
    private static <T> void exportarPDF(TableView<T> tabla, String titulo, File archivo, List<String> filtros) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font fontTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontCabecera = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontDatos = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontNormalBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float pageWidth = PDRectangle.A4.getHeight();
            float pageHeight = PDRectangle.A4.getWidth();

            List<TableColumn<T, ?>> allColumns = obtenerColumnasVisibles(tabla);
            List<T> dataItems = tabla.getItems();

            // Dividir columnas en secciones si son muchas
            List<List<TableColumn<T, ?>>> columnSections = new ArrayList<>();
            for (int i = 0; i < allColumns.size(); i += MAX_COLUMNAS_POR_SECCION) {
                columnSections.add(allColumns.subList(i, Math.min(i + MAX_COLUMNAS_POR_SECCION, allColumns.size())));
            }

            int pageNumber = 1;
            LocalDateTime ahora = LocalDateTime.now();

            // Calcular filas por página (estimado para primera página)
            float espacioFiltros = (filtros != null && !filtros.isEmpty()) ? 25 : 0;
            float espacioInfoGeneral = !informacionGeneralMostrada ? 90 : 30; // Solo en primera página
            float usableHeight = pageHeight - (MARGIN * 2) - 135 - espacioFiltros - espacioInfoGeneral;
            filasPorHojaEstimado = (int) (usableHeight / ROW_HEIGHT);

            // Paginación por filas y columnas
            for (int rowStart = 0; rowStart < dataItems.size(); rowStart += filasPorHojaEstimado) {
                int rowEnd = Math.min(rowStart + filasPorHojaEstimado, dataItems.size());
                List<T> rowBlock = dataItems.subList(rowStart, rowEnd);

                // Calcular filas reales en esta página (puede ser menos que el estimado)
                int filasRealesEnPagina = rowEnd - rowStart;

                for (int sectionIndex = 0; sectionIndex < columnSections.size(); sectionIndex++) {
                    List<TableColumn<T, ?>> section = columnSections.get(sectionIndex);

                    PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                    document.addPage(page);

                    float currentY = pageHeight - MARGIN;
                    PDPageContentStream contentStream = new PDPageContentStream(document, page);

                    // 1. ENCABEZADO - Barra verde con tamaño variable según si es primera página o no
                    boolean esPrimeraPagina = (pageNumber == 1 && sectionIndex == 0);
                    float alturaBarra = esPrimeraPagina ? 60 : 50; // Más grande solo en primera página
                    currentY = dibujarEncabezado(contentStream, fontTitulo, pageWidth, currentY, titulo,
                            rowStart + 1, rowEnd, sectionIndex, columnSections.size(),
                            filasRealesEnPagina, alturaBarra, esPrimeraPagina);

                    // 2. INFORMACIÓN GENERAL (solo en primera página y primera sección)
                    if (!informacionGeneralMostrada && sectionIndex == 0) {
                        currentY = dibujarInformacionGeneral(contentStream, fontNormalBold, fontNormal,
                                pageWidth, currentY, dataItems.size(), ahora, filtros);
                        informacionGeneralMostrada = true;
                    } else if (!esPrimeraPagina) {
                        // Espacio mínimo si no hay información general
                        currentY -= 15;
                    }

                    // 3. TABLA DE DATOS sin bordes
                    currentY = dibujarTablaDatos(contentStream, fontCabecera, fontDatos,
                            pageWidth, currentY, rowBlock, section);

                    contentStream.close();

                    // 4. PIE DE PÁGINA
                    dibujarPiePagina(document, page, fontNormal, pageWidth, pageHeight, pageNumber++);
                }
            }

            document.save(archivo);
        }
    }

    private static float dibujarEncabezado(PDPageContentStream contentStream,
                                           PDType1Font fontTitulo,
                                           float pageWidth,
                                           float currentY,
                                           String titulo,
                                           int filaInicio,
                                           int filaFin,
                                           int seccionIndex,
                                           int totalSecciones,
                                           int filasRealesEnPagina,
                                           float alturaBarra,
                                           boolean esPrimeraPagina) throws IOException {
        // Fondo del encabezado
        contentStream.setNonStrokingColor(COLOR_BANDA_SUPERIOR[0], COLOR_BANDA_SUPERIOR[1], COLOR_BANDA_SUPERIOR[2]);
        contentStream.addRect(MARGIN, currentY - alturaBarra, pageWidth - 2 * MARGIN, alturaBarra);
        contentStream.fill();

        // Título principal - más pequeño si no es primera página
        float tamanioTitulo = esPrimeraPagina ? 20 : 16;
        float posicionTituloY = esPrimeraPagina ? currentY - 30 : currentY - 25;

        contentStream.setNonStrokingColor(1, 1, 1);
        contentStream.beginText();
        contentStream.setFont(fontTitulo, tamanioTitulo);
        contentStream.newLineAtOffset(MARGIN + 10, posicionTituloY);
        contentStream.showText(titulo);
        contentStream.endText();

        // Subtítulo con información de paginación y RANGO DE FILAS
        String subTitulo = "";

        // Información de secciones si hay más de una
        if (totalSecciones > 1) {
            subTitulo = "Sección " + (seccionIndex + 1) + "/" + totalSecciones;
        }

        // Agregar RANGO DE FILAS (siempre)
        String rangoFilas = "Filas: " + filaInicio + "-" + filaFin;

        if (!subTitulo.isEmpty()) {
            subTitulo += " • " + rangoFilas;
        } else {
            subTitulo = rangoFilas;
        }

        // Tamaño de fuente para subtítulo - más pequeño si no es primera página
        float tamanioSubtitulo = esPrimeraPagina ? 12 : 10;
        float posicionSubtituloY = esPrimeraPagina ? currentY - 50 : currentY - 40;

        contentStream.beginText();
        contentStream.setFont(fontTitulo, tamanioSubtitulo);
        contentStream.newLineAtOffset(MARGIN + 10, posicionSubtituloY);
        contentStream.showText(subTitulo);
        contentStream.endText();

        return currentY - (alturaBarra + 20); // Ajustar espacio después de la barra
    }

    private static float dibujarInformacionGeneral(PDPageContentStream contentStream,
                                                   PDType1Font fontBold,
                                                   PDType1Font fontNormal,
                                                   float pageWidth,
                                                   float currentY,
                                                   int totalRegistros,
                                                   LocalDateTime ahora,
                                                   List<String> filtros) throws IOException {
        // Título de sección
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontBold, 12);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText("INFORMACIÓN GENERAL");
        contentStream.endText();

        currentY -= 22;

        // Calcular posición para dos columnas
        float col1X = MARGIN + 10;
        float col2X = pageWidth / 2 + 20;

        // Columna 1 (izquierda) - Información básica
        contentStream.setNonStrokingColor(0, 0, 0);

        // Fecha
        contentStream.beginText();
        contentStream.setFont(fontNormal, 10);
        contentStream.newLineAtOffset(col1X, currentY);
        contentStream.showText("Fecha: " + ahora.format(DATE_FORMATTER));
        contentStream.endText();

        // Hora
        contentStream.beginText();
        contentStream.setFont(fontNormal, 10);
        contentStream.newLineAtOffset(col1X, currentY - 12);
        contentStream.showText("Hora: " + ahora.format(TIME_FORMATTER));
        contentStream.endText();

        // Total de registros
        contentStream.beginText();
        contentStream.setFont(fontNormal, 10);
        contentStream.newLineAtOffset(col1X, currentY - 24);
        contentStream.showText("Total de registros: " + totalRegistros);
        contentStream.endText();

        // Columna 2 (derecha) - Filtros aplicados (si los hay)
        if (filtros != null && !filtros.isEmpty()) {
            // Título de filtros
            contentStream.setNonStrokingColor(COLOR_FILTROS[0], COLOR_FILTROS[1], COLOR_FILTROS[2]);
            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(col2X, currentY);
            contentStream.showText("Filtros aplicados:");
            contentStream.endText();

            // Mostrar cada filtro
            contentStream.setNonStrokingColor(0, 0, 0);
            int maxFiltros = Math.min(3, filtros.size());
            for (int i = 0; i < maxFiltros; i++) {
                String filtro = filtros.get(i);
                if (filtro.length() > 40) {
                    filtro = filtro.substring(0, 37) + "...";
                }

                contentStream.beginText();
                contentStream.setFont(fontNormal, 9);
                contentStream.newLineAtOffset(col2X + 10, currentY - 12 - (i * 12));
                contentStream.showText("• " + filtro);
                contentStream.endText();
            }

            // Si hay más de 3 filtros, mostrar contador
            if (filtros.size() > 3) {
                contentStream.beginText();
                contentStream.setFont(fontNormal, 8);
                contentStream.newLineAtOffset(col2X + 10, currentY - 12 - (3 * 12));
                contentStream.showText("... y " + (filtros.size() - 3) + " más");
                contentStream.endText();
            }
        }

        return currentY - 50;
    }

    private static <T> float dibujarTablaDatos(PDPageContentStream contentStream,
                                               PDType1Font fontCabecera,
                                               PDType1Font fontDatos,
                                               float pageWidth,
                                               float currentY,
                                               List<T> datos,
                                               List<TableColumn<T, ?>> columnas) throws IOException {
        // Definir anchos de columnas
        float tableWidth = pageWidth - 2 * MARGIN;
        float colWidth = tableWidth / columnas.size();

        // Cabecera de la tabla - SIN BORDE, solo fondo
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);

        // Dibujar rectángulo con esquinas redondeadas para encabezado
        float headerHeight = 26; // Ajustado para fuente de 13
        float cornerRadius = 5f;

        // Esquina superior izquierda
        contentStream.moveTo(MARGIN + cornerRadius, currentY);
        // Lado superior
        contentStream.lineTo(MARGIN + tableWidth - cornerRadius, currentY);
        // Esquina superior derecha
        contentStream.curveTo(MARGIN + tableWidth, currentY, MARGIN + tableWidth, currentY,
                MARGIN + tableWidth, currentY - cornerRadius);
        // Lado derecho
        contentStream.lineTo(MARGIN + tableWidth, currentY - headerHeight + cornerRadius);
        // Esquina inferior derecha
        contentStream.curveTo(MARGIN + tableWidth, currentY - headerHeight,
                MARGIN + tableWidth, currentY - headerHeight,
                MARGIN + tableWidth - cornerRadius, currentY - headerHeight);
        // Lado inferior
        contentStream.lineTo(MARGIN + cornerRadius, currentY - headerHeight);
        // Esquina inferior izquierda
        contentStream.curveTo(MARGIN, currentY - headerHeight, MARGIN, currentY - headerHeight,
                MARGIN, currentY - headerHeight + cornerRadius);
        // Lado izquierdo
        contentStream.lineTo(MARGIN, currentY - cornerRadius);
        // Esquina superior izquierda (cerrar)
        contentStream.curveTo(MARGIN, currentY, MARGIN, currentY,
                MARGIN + cornerRadius, currentY);

        contentStream.fill();

        // Dibujar texto de cabecera centrado y en blanco con fuente 13
        contentStream.setNonStrokingColor(1, 1, 1);
        float xPos = MARGIN;
        for (TableColumn<T, ?> col : columnas) {
            String headerText = truncateText(col.getText(), (int) (colWidth - 10));

            // Calcular ancho del texto para centrarlo
            float textWidth = fontCabecera.getStringWidth(headerText) / 1000 * 13; // 13 es el tamaño de fuente

            contentStream.beginText();
            contentStream.setFont(fontCabecera, 13); // CAMBIADO: 13 puntos
            // Centrar texto horizontalmente en la celda
            float textX = xPos + (colWidth - textWidth) / 2;
            contentStream.newLineAtOffset(textX, currentY - 18); // Ajustado
            contentStream.showText(headerText);
            contentStream.endText();
            xPos += colWidth;
        }

        currentY -= 30; // Ajustado por cabecera

        // Dibujar filas de datos sin bordes
        int filaNum = 0;
        for (T item : datos) {
            // Alternar colores de fila
            if (filaNum % 2 == 0) {
                contentStream.setNonStrokingColor(COLOR_FILA_PAR[0], COLOR_FILA_PAR[1], COLOR_FILA_PAR[2]);
            } else {
                contentStream.setNonStrokingColor(COLOR_FILA_IMPAR[0], COLOR_FILA_IMPAR[1], COLOR_FILA_IMPAR[2]);
            }

            // Solo dibujar rectángulo de fondo, sin bordes
            contentStream.addRect(MARGIN, currentY - 26, tableWidth, 26); // Ajustado para fuente 13
            contentStream.fill();

            // Dibujar datos con letra 13 puntos
            contentStream.setNonStrokingColor(0, 0, 0);
            xPos = MARGIN;

            for (TableColumn<T, ?> col : columnas) {
                Object value = col.getCellData(item);
                String text = value != null ? value.toString() : "";

                // Truncar texto si es muy largo
                if (text.length() > 30) {
                    text = text.substring(0, 27) + "...";
                }

                // CALCULAR ANCHO DEL TEXTO PARA CENTRARLO (NUEVO)
                float textWidthData = fontDatos.getStringWidth(text) / 1000 * 13;
                // CENTRAR TEXTO HORIZONTALMENTE EN LA CELDA (NUEVO)
                float textXData = xPos + (colWidth - textWidthData) / 2;

                contentStream.beginText();
                contentStream.setFont(fontDatos, 13); // CAMBIADO: 13 puntos
                // USAR POSICIÓN CENTRADA (MODIFICADO)
                contentStream.newLineAtOffset(textXData, currentY - 20); // Ajustado
                contentStream.showText(text);
                contentStream.endText();

                xPos += colWidth;
            }

            currentY -= 30; // Ajustado para filas con fuente 13
            filaNum++;
        }

        // NO dibujar bordes exteriores de la tabla
        return currentY - 15;
    }

    private static void dibujarPiePagina(PDDocument document, PDPage page, PDType1Font fontNormal,
                                         float pageWidth, float pageHeight, int pageNumber) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {

            // Línea separadora
            contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(MARGIN, MARGIN + 20);
            contentStream.lineTo(pageWidth - MARGIN, MARGIN + 20);
            contentStream.stroke();

            // Texto del pie de página (izquierda)
            contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);
            contentStream.beginText();
            contentStream.setFont(fontNormal, 8);
            contentStream.newLineAtOffset(MARGIN, MARGIN + 5);
            contentStream.showText("Sistema de Gestión de Inventarios GREEP • Reporte generado automáticamente");
            contentStream.endText();

            // Número de página (derecha)
            String paginaTexto = "Página " + pageNumber;
            float textoAncho = fontNormal.getStringWidth(paginaTexto) / 1000 * 8;
            contentStream.beginText();
            contentStream.setFont(fontNormal, 8);
            contentStream.newLineAtOffset(pageWidth - MARGIN - textoAncho - 20, MARGIN + 5);
            contentStream.showText(paginaTexto);
            contentStream.endText();
        }
    }

    // -----------------------
    // Exportar Excel (mantenido igual)
    // -----------------------
    private static <T> void exportarExcel(TableView<T> tabla, String titulo, File archivo, List<String> filtros) throws IOException {
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

        CellStyle filtroStyle = workbook.createCellStyle();
        Font filtroFont = workbook.createFont();
        filtroFont.setBold(false);
        filtroStyle.setFont(filtroFont);

        int rowNum = 0;

        // Título
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(titulo);
        List<TableColumn<T, ?>> columns = obtenerColumnasVisibles(tabla);
        Cell dateCell = titleRow.createCell(columns.size());
        dateCell.setCellValue(LocalDateTime.now());
        dateCell.setCellStyle(dateStyle);

        // Filtros aplicados
        if (filtros != null && !filtros.isEmpty()) {
            Row filtroTitleRow = sheet.createRow(rowNum++);
            filtroTitleRow.createCell(0).setCellValue("Productos filtrados:");
            filtroTitleRow.getCell(0).setCellStyle(filtroStyle);

            for (String filtro : filtros) {
                Row filtroRow = sheet.createRow(rowNum++);
                filtroRow.createCell(0).setCellValue("• " + filtro);
                filtroRow.getCell(0).setCellStyle(filtroStyle);
            }

            rowNum++; // Espacio en blanco
        }

        // Encabezados de columnas
        Row headerRow = sheet.createRow(rowNum++);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns.get(i).getText());
            cell.setCellStyle(headerStyle);
        }

        // Datos
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

    // -----------------------
    // Métodos auxiliares
    // -----------------------
    private static <T> List<TableColumn<T, ?>> obtenerColumnasVisibles(TableView<T> tabla) {
        List<TableColumn<T, ?>> columnas = new ArrayList<>();
        for (TableColumn<T, ?> col : tabla.getColumns()) {
            if (col.isVisible()) {
                columnas.add(col);
            }
        }
        return columnas;
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) return text.substring(0, maxLength - 3) + "...";
        return text;
    }

    private static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText("Exportación exitosa");
        configurarMensajeLargo(alert, mensaje);
        alert.showAndWait();
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al exportar");
        configurarMensajeLargo(alert, mensaje);
        alert.showAndWait();
    }

    private static void configurarMensajeLargo(Alert alert, String mensaje) {
        TextArea area = new TextArea(mensaje != null ? mensaje : "");
        area.setWrapText(true);
        area.setEditable(false);
        area.setFocusTraversable(false);
        area.setMaxWidth(Double.MAX_VALUE);
        area.setMaxHeight(Double.MAX_VALUE);

        VBox contenedor = new VBox(area);
        VBox.setVgrow(area, Priority.ALWAYS);
        contenedor.setPrefWidth(760);
        contenedor.setPrefHeight(320);

        alert.getDialogPane().setContent(contenedor);
        alert.getDialogPane().setPrefSize(780, 360);
        alert.setResizable(true);
    }
}
