package Compartido.importar;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import conexion.Conexion;

public class importador {

    public static void importarExcel(String tabla, String columnaId) {
        // AVISO DE CONFIRMACIÓN ANTES DE IMPORTAR
        Alert aviso = new Alert(Alert.AlertType.CONFIRMATION);
        aviso.setTitle("Importación");
        aviso.setHeaderText("Aviso de sobrescritura");
        aviso.setContentText("Algunos registros existentes podrían modificarse al importar.\n¿Desea continuar?");
        if (aviso.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return; // Usuario canceló, no continuar
        }

        // Selección de archivo Excel
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File archivo = fileChooser.showOpenDialog(null);
        if (archivo == null) return;

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(fis);
             Connection conn = new Conexion().conectar()) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            // Encabezado en la segunda fila
            Row headerRow = sheet.getRow(1);
            if (headerRow == null) {
                mostrarError("No se encontró la fila de encabezado en la segunda fila.");
                return;
            }

            // Obtener nombres de columnas
            List<String> columnas = new ArrayList<>();
            for (Cell cell : headerRow) {
                String colName = getCellAsString(cell).trim();
                if (!colName.isEmpty()) columnas.add(colName);
            }

            // Índice de columna ID
            int idxColumnaId = -1;
            for (int i = 0; i < columnas.size(); i++) {
                if (columnas.get(i).equalsIgnoreCase(columnaId)) {
                    idxColumnaId = i;
                    break;
                }
            }
            if (idxColumnaId == -1) {
                mostrarError("No se encontró la columna '" + columnaId + "' en la segunda fila.");
                return;
            }

            // Construir SQL dinámico con ON DUPLICATE KEY UPDATE
            StringBuilder sql = new StringBuilder("INSERT INTO ").append(tabla).append(" (");
            for (String col : columnas) sql.append("`").append(col).append("`,");
            sql.deleteCharAt(sql.length() - 1).append(") VALUES (");
            for (int i = 0; i < columnas.size(); i++) sql.append("?,");
            sql.deleteCharAt(sql.length() - 1).append(") ON DUPLICATE KEY UPDATE ");

            for (int i = 0; i < columnas.size(); i++) {
                if (i != idxColumnaId) sql.append("`").append(columnas.get(i)).append("`=VALUES(`").append(columnas.get(i)).append("`),");
            }
            sql.deleteCharAt(sql.length() - 1); // eliminar última coma

            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            // Procesar filas desde la tercera
            for (int i = 2; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Object[] values = new Object[columnas.size()];
                for (int j = 0; j < columnas.size(); j++) {
                    Cell cell = row.getCell(j);
                    boolean isId = (j == idxColumnaId);
                    values[j] = getCellValue(cell, isId);

                    if (isId && values[j] == null) {
                        mostrarError("Fila " + (i + 1) + ": La columna '" + columnaId + "' no puede estar vacía.");
                        return;
                    }
                }

                // Asignar parámetros y ejecutar
                for (int j = 0; j < columnas.size(); j++) {
                    stmt.setObject(j + 1, values[j]);
                }
                stmt.executeUpdate();
            }

            mostrarExito("Importación finalizada correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al importar: " + e.getMessage());
        }
    }

    private static Object getCellValue(Cell cell, boolean forId) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                String str = cell.getStringCellValue().trim();
                if (forId && str.matches("\\d+")) return Integer.parseInt(str);
                return str.isEmpty() ? null : str;
            case NUMERIC:
                if (forId) return (int) cell.getNumericCellValue();
                return DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
            case BOOLEAN: return cell.getBooleanCellValue();
            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        String fStr = cell.getStringCellValue().trim();
                        if (forId && fStr.matches("\\d+")) return Integer.parseInt(fStr);
                        return fStr.isEmpty() ? null : fStr;
                    case NUMERIC:
                        if (forId) return (int) cell.getNumericCellValue();
                        return DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
                    case BOOLEAN: return cell.getBooleanCellValue();
                    default: return null;
                }
            default: return null;
        }
    }

    private static String getCellAsString(Cell cell) {
        Object value = getCellValue(cell, false);
        return value != null ? value.toString() : "";
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
