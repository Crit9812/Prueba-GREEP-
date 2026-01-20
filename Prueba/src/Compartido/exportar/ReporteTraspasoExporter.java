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

    // Formatters
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Colores (RGB convertidos de HEX)
    // #91d485 -> RGB: 145/212/133 = 0.5686, 0.8314, 0.5216
    private static final float[] COLOR_BANDA_SUPERIOR = {0.5686f, 0.8314f, 0.5216f}; // Verde #91d485

    // #333 -> RGB: 51/51/51 = 0.2, 0.2, 0.2
    private static final float[] COLOR_SUBTITULOS = {0.2f, 0.2f, 0.2f}; // Gris oscuro #333

    // #0274be -> RGB: 2/116/190 = 0.0078, 0.4549, 0.7451
    private static final float[] COLOR_PRODUCTOS = {0.0078f, 0.4549f, 0.7451f}; // Azul #0274be

    // #d9534f -> RGB: 217/83/79 = 0.8510, 0.3255, 0.3098 (Rojo para totales)
    private static final float[] COLOR_TOTALES = {0.8510f, 0.3255f, 0.3098f}; // Rojo #d9534f

    // Otros colores
    private static final float[] COLOR_ENCABEZADO_TABLA = {0.95f, 0.95f, 0.95f};  // Gris claro
    private static final float[] COLOR_FILA_PAR = {0.98f, 0.98f, 0.98f};  // Gris muy claro
    private static final float[] COLOR_FILA_IMPAR = {1.0f, 1.0f, 1.0f};   // Blanco

    // Constantes de diseño
    private static final float MARGIN = 40f;
    private static final float LINE_HEIGHT = 14f;
    private static final float SECTION_SPACING = 20f;
    private static final float CELL_PADDING = 5f;
    private static final float ROW_HEIGHT_DETALLES = 30f; // Aumentado para fuente 14
    private static final float ROW_HEIGHT_UBICACIONES = 25f;

    // Variables para control de páginas
    private static int filasPorHojaActual = 0;
    private static int totalFilasEnHoja = 0;

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
        File archivo = new File(carpeta, "Reporte_Traspaso_" + claveEntrada + "_" + fechaHora + ".pdf");

        try {
            generarReporte(archivo, claveEntrada, detalles, ubicacionesPorProducto);
            mostrarExito("✅ Reporte generado exitosamente\n\n" +
                    "Archivo: Reporte_Traspaso_" + claveEntrada + "_" + fechaHora + ".pdf\n" +
                    "Ubicación: " + archivo.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("❌ Error al generar el reporte:\n" + e.getMessage());
        }
    }

    private static void generarReporte(File archivo,
                                       String claveEntrada,
                                       List<model.DetalleEntrada> detalles,
                                       Map<String, List<UbicacionCompra>> ubicacionesPorProducto) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Resetear variables para cada reporte
            filasPorHojaActual = 0;
            totalFilasEnHoja = 0;

            // Calcular cuántas filas caben en una página
            PDRectangle pageSize = PDRectangle.LETTER;
            float pageHeight = pageSize.getHeight();
            float espacioRestante = pageHeight - (MARGIN * 2) - 180; // Restar encabezado e info general
            int maxFilasPorHoja = (int) (espacioRestante / ROW_HEIGHT_DETALLES);

            // Dividir detalles en páginas si son muchos
            int totalPaginas = (int) Math.ceil((double) detalles.size() / maxFilasPorHoja);

            for (int pagina = 0; pagina < totalPaginas; pagina++) {
                int inicio = pagina * maxFilasPorHoja;
                int fin = Math.min(inicio + maxFilasPorHoja, detalles.size());
                List<model.DetalleEntrada> detallesPagina = detalles.subList(inicio, fin);

                PDPage page = new PDPage(pageSize);
                document.addPage(page);
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                // Posiciones iniciales
                float pageWidth = pageSize.getWidth();
                float currentY = pageHeight - MARGIN;

                // Fuentes
                PDType1Font fontTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontSubtitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font fontNormalBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontTablaCabecera = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontTablaDatos = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // 1. ENCABEZADO DEL REPORTE con filas por hoja
                totalFilasEnHoja = detallesPagina.size();
                currentY = dibujarEncabezado(contentStream, fontTitulo, pageWidth, currentY,
                        claveEntrada, pagina + 1, totalPaginas, totalFilasEnHoja);

                // 2. INFORMACIÓN GENERAL (solo en primera página)
                if (pagina == 0) {
                    String quienEnvia = "";
                    if (!detalles.isEmpty()) {
                        quienEnvia = obtenerNombreSucursal(detalles.get(0));
                    }
                    currentY = dibujarInformacionGeneral(contentStream, fontSubtitulo, fontNormal,
                            pageWidth, currentY, detalles, quienEnvia);
                } else {
                    currentY -= 20; // Espacio sin información general
                }

                // 3. TABLA DE DETALLES (con fuente 14 para registros)
                currentY = dibujarTablaDetalles(contentStream, fontTablaCabecera, fontTablaDatos,
                        pageWidth, currentY, detallesPagina, inicio + 1);

                // 4. UBICACIONES POR PRODUCTO (solo en la última página)
                if (pagina == totalPaginas - 1) {
                    currentY = dibujarUbicaciones(contentStream, fontSubtitulo, fontNormal, fontNormalBold,
                            pageWidth, currentY, ubicacionesPorProducto, detalles);
                }

                // 5. PIE DE PÁGINA
                dibujarPiePagina(contentStream, fontNormal, pageWidth, pageHeight, pagina + 1, totalPaginas);

                contentStream.close();
            }

            document.save(archivo);
        }
    }

    private static float dibujarEncabezado(PDPageContentStream contentStream,
                                           PDType1Font fontTitulo,
                                           float pageWidth,
                                           float currentY,
                                           String claveEntrada,
                                           int paginaActual,
                                           int totalPaginas,
                                           int filasEnEstaHoja) throws IOException {
        // Fondo del encabezado con el nuevo color verde #91d485
        contentStream.setNonStrokingColor(COLOR_BANDA_SUPERIOR[0], COLOR_BANDA_SUPERIOR[1], COLOR_BANDA_SUPERIOR[2]);
        contentStream.addRect(MARGIN, currentY - 60, pageWidth - 2 * MARGIN, 60);
        contentStream.fill();

        // Título
        contentStream.setNonStrokingColor(1, 1, 1); // Blanco
        contentStream.beginText();
        contentStream.setFont(fontTitulo, 20);
        contentStream.newLineAtOffset(MARGIN + 10, currentY - 30);
        contentStream.showText("REPORTE DE TRASPASO");
        contentStream.endText();

        // Subtítulo con clave y filas por hoja
        String subtitulo = "Clave: " + claveEntrada;
        if (totalPaginas > 1) {
            subtitulo += " • Página " + paginaActual + "/" + totalPaginas;
        }
        subtitulo += " • Filas en hoja: " + filasEnEstaHoja;

        contentStream.beginText();
        contentStream.setFont(fontTitulo, 14);
        contentStream.newLineAtOffset(MARGIN + 10, currentY - 50);
        contentStream.showText(subtitulo);
        contentStream.endText();

        return currentY - 80;
    }

    private static float dibujarInformacionGeneral(PDPageContentStream contentStream,
                                                   PDType1Font fontSubtitulo,
                                                   PDType1Font fontNormal,
                                                   float pageWidth,
                                                   float currentY,
                                                   List<model.DetalleEntrada> detalles,
                                                   String quienEnvia) throws IOException {
        LocalDateTime ahora = LocalDateTime.now();

        // Título de sección en color #333
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText("INFORMACIÓN GENERAL");
        contentStream.endText();

        currentY -= 25;

        // Calcular totales
        int totalProductos = detalles.size();
        int totalCantidad = 0;
        float totalPrecio = 0;

        for (model.DetalleEntrada detalle : detalles) {
            try {
                totalCantidad += Integer.parseInt(detalle.getCantidad());
                String precioStr = detalle.getPrecioTotal().replace("$", "").replace(",", "").trim();
                totalPrecio += Float.parseFloat(precioStr);
            } catch (NumberFormatException e) {
                System.err.println("Error al parsear número: " + e.getMessage());
            }
        }

        // Columna 1 (izquierda) - información básica en el orden solicitado
        float col1X = MARGIN + 10;
        contentStream.setNonStrokingColor(0, 0, 0); // Negro

        // 1. Fecha
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY);
        contentStream.showText("Fecha: " + ahora.format(DATE_FORMATTER));
        contentStream.endText();

        // 2. Hora
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - LINE_HEIGHT);
        contentStream.showText("Hora: " + ahora.format(TIME_FORMATTER));
        contentStream.endText();

        // 3. Quién envía el traspaso
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (2 * LINE_HEIGHT));
        contentStream.showText("Remitente: " + quienEnvia);
        contentStream.endText();

        // 4. Total de productos
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (3 * LINE_HEIGHT));
        contentStream.showText("Total de productos: " + totalProductos);
        contentStream.endText();

        // 5. Cantidad total
        contentStream.beginText();
        contentStream.setFont(fontNormal, 11);
        contentStream.newLineAtOffset(col1X, currentY - (4 * LINE_HEIGHT));
        contentStream.showText("Cantidad total: " + totalCantidad + " unidades");
        contentStream.endText();

        // Columna 2 (derecha) - valor total alineado a la derecha
        String valorTotalTexto = "Valor total: $" + String.format("%,.2f", totalPrecio);
        float textoAncho = fontSubtitulo.getStringWidth(valorTotalTexto) / 1000 * 12; // Tamaño 12

        // Calcular posición X para alinear a la derecha
        float col2X = pageWidth - MARGIN - textoAncho - 20;

        contentStream.setNonStrokingColor(COLOR_TOTALES[0], COLOR_TOTALES[1], COLOR_TOTALES[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 12);
        contentStream.newLineAtOffset(col2X, currentY - 5);
        contentStream.showText(valorTotalTexto);
        contentStream.endText();

        return currentY - 90; // Ajustado por las líneas adicionales
    }

    private static float dibujarTablaDetalles(PDPageContentStream contentStream,
                                              PDType1Font fontCabecera,
                                              PDType1Font fontDatos,
                                              float pageWidth,
                                              float currentY,
                                              List<model.DetalleEntrada> detalles,
                                              int inicioNumeracion) throws IOException {
        // Título de sección en color #333
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontCabecera, 14);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText("DETALLES DE PRODUCTOS");
        contentStream.endText();

        currentY -= 25;

        // Definir anchos de columnas
        float[] columnWidths = {40, 90, 180, 70, 90, 90};
        float tableWidth = pageWidth - 2 * MARGIN;
        float[] scaledWidths = escalarAnchosColumnas(columnWidths, tableWidth);

        // Cabecera de la tabla
        String[] headers = {"#", "Clave", "Producto", "Cantidad", "Precio Unit.", "Precio Total"};

        // Dibujar fondo de cabecera
        contentStream.setNonStrokingColor(COLOR_ENCABEZADO_TABLA[0], COLOR_ENCABEZADO_TABLA[1], COLOR_ENCABEZADO_TABLA[2]);
        contentStream.addRect(MARGIN, currentY - 25, tableWidth, 25); // Aumentado de 20 a 25 para fuente 14
        contentStream.fill();

        // Dibujar texto de cabecera
        contentStream.setNonStrokingColor(0, 0, 0);
        float xPos = MARGIN;
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.setFont(fontCabecera, 10);
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 18); // Ajustado
            contentStream.showText(headers[i]);
            contentStream.endText();
            xPos += scaledWidths[i];
        }

        currentY -= 30; // Aumentado de 25 a 30

        // Dibujar filas de datos CON FUENTE 14
        int filaNum = inicioNumeracion;
        for (model.DetalleEntrada detalle : detalles) {
            // Alternar colores de fila
            if ((filaNum - inicioNumeracion + 1) % 2 == 0) {
                contentStream.setNonStrokingColor(COLOR_FILA_PAR[0], COLOR_FILA_PAR[1], COLOR_FILA_PAR[2]);
            } else {
                contentStream.setNonStrokingColor(COLOR_FILA_IMPAR[0], COLOR_FILA_IMPAR[1], COLOR_FILA_IMPAR[2]);
            }
            contentStream.addRect(MARGIN, currentY - 25, tableWidth, 25); // Aumentado de 20 a 25
            contentStream.fill();

            // Dibujar datos CON FUENTE 14
            contentStream.setNonStrokingColor(0, 0, 0);
            xPos = MARGIN;

            // Columna 1: Número
            contentStream.beginText();
            contentStream.setFont(fontDatos, 14); // CAMBIADO DE 10 A 14
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 18); // Ajustado
            contentStream.showText(String.valueOf(filaNum));
            contentStream.endText();
            xPos += scaledWidths[0];

            // Columna 2: Clave
            contentStream.beginText();
            contentStream.setFont(fontDatos, 14); // CAMBIADO DE 10 A 14
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 18);
            contentStream.showText(detalle.getClaveProducto());
            contentStream.endText();
            xPos += scaledWidths[1];

            // Columna 3: Producto (ajustar si es muy largo)
            String producto = detalle.getProducto();
            if (producto.length() > 20) { // Reducido de 25 para fuente más grande
                producto = producto.substring(0, 17) + "...";
            }
            contentStream.beginText();
            contentStream.setFont(fontDatos, 14); // CAMBIADO DE 10 A 14
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 18);
            contentStream.showText(producto);
            contentStream.endText();
            xPos += scaledWidths[2];

            // Columna 4: Cantidad
            contentStream.beginText();
            contentStream.setFont(fontDatos, 14); // CAMBIADO DE 10 A 14
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 18);
            contentStream.showText(detalle.getCantidad());
            contentStream.endText();
            xPos += scaledWidths[3];

            // Columna 5: Precio Unitario
            contentStream.beginText();
            contentStream.setFont(fontDatos, 14); // CAMBIADO DE 10 A 14
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 18);
            contentStream.showText(detalle.getPrecioUnitario());
            contentStream.endText();
            xPos += scaledWidths[4];

            // Columna 6: Precio Total
            contentStream.setNonStrokingColor(COLOR_TOTALES[0], COLOR_TOTALES[1], COLOR_TOTALES[2]);
            contentStream.beginText();
            contentStream.setFont(fontCabecera, 14); // CAMBIADO DE 10 A 14
            contentStream.newLineAtOffset(xPos + CELL_PADDING, currentY - 18);
            contentStream.showText(detalle.getPrecioTotal());
            contentStream.endText();

            currentY -= 30; // Aumentado de 25 a 30 para fuente más grande
            filaNum++;

            // Actualizar contador de filas en esta hoja
            filasPorHojaActual++;
        }

        // Dibujar bordes de la tabla
        contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
        contentStream.setLineWidth(0.5f);
        contentStream.addRect(MARGIN, currentY + 5, tableWidth, (detalles.size() + 1) * 30);
        contentStream.stroke();

        return currentY - SECTION_SPACING;
    }

    private static float dibujarUbicaciones(PDPageContentStream contentStream,
                                            PDType1Font fontSubtitulo,
                                            PDType1Font fontNormal,
                                            PDType1Font fontNormalBold,
                                            float pageWidth,
                                            float currentY,
                                            Map<String, List<UbicacionCompra>> ubicacionesPorProducto,
                                            List<model.DetalleEntrada> detalles) throws IOException {
        if (ubicacionesPorProducto == null || ubicacionesPorProducto.isEmpty()) {
            return currentY;
        }

        // Verificar si hay espacio suficiente
        float espacioMinimoNecesario = 100f;
        if (currentY < MARGIN + espacioMinimoNecesario) {
            // No hay espacio suficiente, retornar posición actual
            return currentY;
        }

        // Título de sección en color #333
        contentStream.setNonStrokingColor(COLOR_SUBTITULOS[0], COLOR_SUBTITULOS[1], COLOR_SUBTITULOS[2]);
        contentStream.beginText();
        contentStream.setFont(fontSubtitulo, 14);
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText("UBICACIONES ASIGNADAS");
        contentStream.endText();

        currentY -= 25;

        // Para cada producto con ubicaciones
        for (model.DetalleEntrada detalle : detalles) {
            List<UbicacionCompra> ubicaciones = ubicacionesPorProducto.get(detalle.getClaveProducto());

            if (ubicaciones != null && !ubicaciones.isEmpty()) {
                // Verificar si hay espacio para este producto
                float espacioProducto = 20 + (ubicaciones.size() * 25) + 10;
                if (currentY - espacioProducto < MARGIN + 50) {
                    break; // No hay espacio suficiente
                }

                // Nombre del producto en color azul #0274be
                contentStream.setNonStrokingColor(COLOR_PRODUCTOS[0], COLOR_PRODUCTOS[1], COLOR_PRODUCTOS[2]);
                contentStream.beginText();
                contentStream.setFont(fontNormalBold, 11);
                contentStream.newLineAtOffset(MARGIN + 10, currentY);
                contentStream.showText("• " + detalle.getProducto() + " (" + detalle.getClaveProducto() + ")");
                contentStream.endText();

                currentY -= 20;

                // Encabezado de ubicaciones
                float tablaUbicacionesWidth = pageWidth - 2 * MARGIN - 40;
                float[] columnWidthsUbic = {200, 100};
                float[] scaledUbicWidths = escalarAnchosColumnas(columnWidthsUbic, tablaUbicacionesWidth);

                // Fondo cabecera
                contentStream.setNonStrokingColor(COLOR_ENCABEZADO_TABLA[0], COLOR_ENCABEZADO_TABLA[1], COLOR_ENCABEZADO_TABLA[2]);
                contentStream.addRect(MARGIN + 20, currentY - 20, tablaUbicacionesWidth, 20);
                contentStream.fill();

                // Texto cabecera
                contentStream.setNonStrokingColor(0, 0, 0);
                float xPosUbic = MARGIN + 20;

                contentStream.beginText();
                contentStream.setFont(fontNormalBold, 9);
                contentStream.newLineAtOffset(xPosUbic + 5, currentY - 15);
                contentStream.showText("Ubicación");
                contentStream.endText();
                xPosUbic += scaledUbicWidths[0];

                contentStream.beginText();
                contentStream.setFont(fontNormalBold, 9);
                contentStream.newLineAtOffset(xPosUbic + 5, currentY - 15);
                contentStream.showText("Cantidad");
                contentStream.endText();

                currentY -= 25;

                // Datos de ubicaciones
                int ubicFilaNum = 1;
                for (UbicacionCompra ubicacion : ubicaciones) {
                    // Alternar colores
                    if (ubicFilaNum % 2 == 0) {
                        contentStream.setNonStrokingColor(COLOR_FILA_PAR[0], COLOR_FILA_PAR[1], COLOR_FILA_PAR[2]);
                    } else {
                        contentStream.setNonStrokingColor(COLOR_FILA_IMPAR[0], COLOR_FILA_IMPAR[1], COLOR_FILA_IMPAR[2]);
                    }
                    contentStream.addRect(MARGIN + 20, currentY - 20, tablaUbicacionesWidth, 20);
                    contentStream.fill();

                    // Datos
                    contentStream.setNonStrokingColor(0, 0, 0);
                    xPosUbic = MARGIN + 20;

                    contentStream.beginText();
                    contentStream.setFont(fontNormal, 9);
                    contentStream.newLineAtOffset(xPosUbic + 5, currentY - 15);
                    contentStream.showText(ubicacion.getUbicacion());
                    contentStream.endText();
                    xPosUbic += scaledUbicWidths[0];

                    contentStream.beginText();
                    contentStream.setFont(fontNormal, 9);
                    contentStream.newLineAtOffset(xPosUbic + 5, currentY - 15);
                    contentStream.showText(String.valueOf(ubicacion.getCantidad()));
                    contentStream.endText();

                    currentY -= 25;
                    ubicFilaNum++;
                }

                currentY -= 10;
            }
        }

        return currentY;
    }

    private static void dibujarPiePagina(PDPageContentStream contentStream,
                                         PDType1Font fontNormal,
                                         float pageWidth,
                                         float pageHeight,
                                         int paginaActual,
                                         int totalPaginas) throws IOException {
        // Línea separadora
        contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, MARGIN + 20);
        contentStream.lineTo(pageWidth - MARGIN, MARGIN + 20);
        contentStream.stroke();

        // Texto del pie de página actualizado
        contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);
        contentStream.beginText();
        contentStream.setFont(fontNormal, 9);
        contentStream.newLineAtOffset(MARGIN, MARGIN + 5);
        contentStream.showText("Sistema de Gestión de Inventarios GREEP - Reporte generado automáticamente");
        contentStream.endText();

        // Número de página
        String paginaTexto = "Página " + paginaActual + " de " + totalPaginas;
        float textoAncho = fontNormal.getStringWidth(paginaTexto) / 1000 * 9;
        contentStream.beginText();
        contentStream.setFont(fontNormal, 9);
        contentStream.newLineAtOffset(pageWidth - MARGIN - textoAncho - 20, MARGIN + 5);
        contentStream.showText(paginaTexto);
        contentStream.endText();
    }

    private static float[] escalarAnchosColumnas(float[] widths, float totalWidth) {
        float[] scaled = new float[widths.length];
        float sum = 0;

        for (float width : widths) {
            sum += width;
        }

        float scaleFactor = totalWidth / sum;

        for (int i = 0; i < widths.length; i++) {
            scaled[i] = widths[i] * scaleFactor;
        }

        return scaled;
    }

    private static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText("Reporte generado");
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefSize(500, 200);
        alert.showAndWait();
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al generar reporte");
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefSize(500, 200);
        alert.showAndWait();
    }

    private static String obtenerNombreSucursal(model.DetalleEntrada detalle) {
        return detalle.getNombreSucursal() != null && !detalle.getNombreSucursal().isEmpty()
                ? detalle.getNombreSucursal()
                : "No especificado";
    }
}