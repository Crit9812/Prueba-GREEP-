package Compartido.model.DAO;

import conexion.Conexion;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

public class GenericDAO<T> {

    private final Class<T> type;
    private final Connection conexion;

    public GenericDAO(Class<T> type) {
        this.type = type;
        this.conexion = new Conexion().conectar();
    }

    // --------------------- Identificar elementos de la tabla -------------------

    private String getTableName() {
        Table table = type.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("La clase " + type.getSimpleName() + " debe tener @Table");
        }
        return table.name();
    }

    private Field getPrimaryKeyField() {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new RuntimeException("No se encontró campo con @PrimaryKey en " + type.getSimpleName());
    }

    private String getPrimaryKeyColumn() {
        Field pkField = getPrimaryKeyField();
        Column col = pkField.getAnnotation(Column.class);
        return col != null ? col.name() : pkField.getName();
    }

    private ArrayList<Field> getColumns() {
        ArrayList<Field> cols = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(PrimaryKey.class)) {
                field.setAccessible(true);
                cols.add(field);
            }
        }
        return cols;
    }

    private String getColumnName(Field field) {
        Column col = field.getAnnotation(Column.class);
        return (col != null) ? col.name() : field.getName();
    }

    private T mapResultSet(ResultSet rs) throws Exception {
        Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        T instancia = constructor.newInstance();

        for (Field field : getColumns()) {
            String colName = getColumnName(field);
            // Usamos getObject y dejamos que la asignación mediante reflection convierta si es String
            Object value = rs.getObject(colName);
            if (value != null) {
                // Convertimos a String si el campo es String; si no, intentamos asignar directamente
                if (field.getType().equals(String.class)) {
                    field.set(instancia, value.toString());
                } else {
                    field.set(instancia, value);
                }
            } else {
                field.set(instancia, null);
            }
        }
        return instancia;
    }

    // --------------------- CRUD básico -------------------

    public boolean insertar(T entidad) {
        try {
            Field pkField = getPrimaryKeyField();
            ArrayList<Field> allColumns = getColumns();

            // Construimos la lista de columnas que sí vamos a insertar (si pk es null, la omitimos)
            ArrayList<Field> insertColumns = new ArrayList<>();
            for (Field f : allColumns) {
                Object val = f.get(entidad);
                if (f.equals(pkField) && (val == null || (val instanceof String && ((String) val).isBlank()))) {
                    // omitimos la PK si su valor es null/blank -> dejar que la BD la genere
                    continue;
                }
                insertColumns.add(f);
            }

            // Si no hay columnas para insertar (raro), devolvemos false
            if (insertColumns.isEmpty()) {
                throw new RuntimeException("No hay columnas para insertar en " + getTableName());
            }

            String columnNames = insertColumns.stream()
                    .map(this::getColumnName)
                    .collect(Collectors.joining(","));

            String placeholders = insertColumns.stream()
                    .map(c -> "?")
                    .collect(Collectors.joining(","));

            String sql = "INSERT INTO " + getTableName() +
                    " (" + columnNames + ") VALUES (" + placeholders + ")";

            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                int index = 1;
                for (Field field : insertColumns) {
                    Object value = field.get(entidad);
                    if (value == null) {
                        ps.setNull(index++, Types.NULL);
                    } else {
                        ps.setObject(index++, value);
                    }
                }

                int updated = ps.executeUpdate();

                // Si la PK era auto-generada y la dejamos fuera del INSERT, intentamos recuperar el generated key
                if (updated > 0) {
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk.next()) {
                            Object generated = gk.getObject(1);
                            // Si la PK es String en la entidad, convertimos a String, si es Integer, seteamos Integer...
                            Field pk = pkField;
                            if (generated != null) {
                                if (pk.getType().equals(String.class)) {
                                    pk.set(entidad, generated.toString());
                                } else if (pk.getType().equals(Integer.class) || pk.getType().equals(int.class)) {
                                    pk.set(entidad, ((Number) generated).intValue());
                                } else {
                                    pk.set(entidad, generated);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // no fatal, seguimos
                    }
                }

                return updated > 0;
            }

        } catch (Exception e) {
            System.out.println("Error en insertar: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<T> obtenerTodos() {
        ArrayList<T> lista = new ArrayList<>();
        String sql = "SELECT * FROM " + getTableName();

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapResultSet(rs));
            }
        } catch (Exception e) {
            System.out.println("Error en obtenerTodos: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizar(T entidad) {
        try {
            Field pkField = getPrimaryKeyField();
            String pkColumn = getColumnName(pkField);

            ArrayList<Field> columns = getColumns();

            String setClause = columns.stream()
                    .filter(f -> !f.equals(pkField))
                    .map(f -> getColumnName(f) + "=?")
                    .collect(Collectors.joining(","));

            String sql = "UPDATE " + getTableName() +
                    " SET " + setClause +
                    " WHERE " + pkColumn + "=?";

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {

                int index = 1;
                Object pkValue = null;

                for (Field field : columns) {
                    Object value = field.get(entidad);

                    if (field.equals(pkField)) {
                        pkValue = value;
                    } else {
                        if (value == null) {
                            ps.setNull(index++, Types.NULL);
                        } else {
                            ps.setObject(index++, value);
                        }
                    }
                }

                if (pkValue == null) {
                    throw new RuntimeException("PK no puede ser null al actualizar en " + getTableName());
                }

                ps.setObject(index, pkValue);

                return ps.executeUpdate() > 0;
            }

        } catch (Exception e) {
            System.out.println("Error en actualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM " + getTableName() +
                " WHERE " + getPrimaryKeyColumn() + "=?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error en eliminar: " + e.getMessage());
            return false;
        }
    }

    // --------------------- Búsquedas avanzadas -------------------

    public T buscarExacto(String campo, Object valor) {
        if (valor == null) return null;

        String valorStr = valor.toString();

        String sql = "SELECT * FROM " + getTableName() + " WHERE LOWER(" + campo + ") = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, valorStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (Exception e) {
            System.out.println("Error en buscarExacto: " + e.getMessage());
        }
        return null;
    }

    public ArrayList<T> buscarParcial(String campo, String valor) {
        ArrayList<T> lista = new ArrayList<>();
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + campo + " LIKE ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, "%" + valor + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSet(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("Error en buscarListaPorCampoLike: " + e.getMessage());
        }
        return lista;
    }

    public ArrayList<T> buscarMultiple(String campo1, String campo2, String valor) {
        ArrayList<T> lista = new ArrayList<>();
        if (valor == null || valor.trim().isEmpty()) {
            return obtenerTodos();
        }

        String busqueda = valor.trim().toLowerCase();
        String sql = "SELECT * FROM " + getTableName() +
                " WHERE LOWER(" + campo1 + ") LIKE ? OR LOWER(" + campo2 + ") LIKE ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, "%" + busqueda + "%");
            ps.setString(2, "%" + busqueda + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSet(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("Error en busquedaMultiple: " + e.getMessage());
        }
        return lista;
    }

    // --------------------- Métodos auxiliares -------------------

    // Metodo generalizado para construir descripciones concatenando campos
    public static String construirDescripcion(String... campos) {
        if (campos == null || campos.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String campo : campos) {
            if (campo != null && !campo.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(campo.trim());
            }
        }

        return sb.toString();
    }

    // --------------------- Métodos  -------------------

    public ArrayList<Map<String, String>> obtenerProductosConMarcaEtiqueta() {
        ArrayList<Map<String, String>> resultados = new ArrayList<>();

        String sql = """
        SELECT
            p.id,
            p.nombre,
            p.descripcion,
            m.nombre AS marca,
            e.nombre AS etiqueta
        FROM productos p
        LEFT JOIN marcas m ON m.id = p.marca
        LEFT JOIN etiquetas e ON e.id = p.etiqueta
        ORDER BY p.nombre
    """;

        try (PreparedStatement ps = this.conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, String> producto = new HashMap<>();

                // Usar el método generalizado para construir descripción
                String descripcionCompleta = construirDescripcion(
                        rs.getString("marca"),
                        rs.getString("etiqueta")
                );

                producto.put("id", rs.getString("id"));
                producto.put("nombre", rs.getString("nombre"));
                producto.put("descripcion", descripcionCompleta);

                resultados.add(producto);
            }

        } catch (Exception e) {
            System.out.println("Error en obtenerProductosConMarcaEtiqueta: " + e.getMessage());
            e.printStackTrace();
        }

        return resultados;
    }

    public static class ValidacionLoteSalida {
        private final boolean entradaCompletada;
        private final boolean tieneSalida;

        public ValidacionLoteSalida(boolean entradaCompletada, boolean tieneSalida) {
            this.entradaCompletada = entradaCompletada;
            this.tieneSalida = tieneSalida;
        }

        public boolean isEntradaCompletada() {
            return entradaCompletada;
        }

        public boolean isTieneSalida() {
            return tieneSalida;
        }
    }

    public static ValidacionLoteSalida validarLoteTraspasoSalida(String idProducto, String lote) {
        if (idProducto == null || idProducto.isBlank() || lote == null || lote.isBlank()) {
            return null;
        }

        try (Connection conn = new Conexion().conectar()) {
            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

            String colEntradaId = resolverColumna(columnasEntradas, "id", "claveEntrada", "idEntrada", "entrada_id");
            String colEntradaEstado = resolverColumna(columnasEntradas, "Estado", "estado");

            String colDetalleEntradaId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
            String colDetalleEntradaEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalle, "claveProducto", "idProducto", "id_producto", "producto_id");

            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                    "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");

            if (colEntradaId == null || colEntradaEstado == null
                    || colDetalleEntradaId == null || colDetalleEntradaEntrada == null || colDetalleEntradaProducto == null
                    || colArticuloDetalleEntrada == null || colArticuloLote == null) {
                return null;
            }

            String sqlEstado = """
                SELECT e.`%s` AS estado
                FROM articulo a
                JOIN detalle_Entrada de ON de.`%s` = a.`%s`
                JOIN entradas e ON e.`%s` = de.`%s`
                WHERE a.`%s` = ? AND de.`%s` = ?
                ORDER BY e.`%s` DESC
                LIMIT 1
            """.formatted(colEntradaEstado, colDetalleEntradaId, colArticuloDetalleEntrada,
                    colEntradaId, colDetalleEntradaEntrada, colArticuloLote, colDetalleEntradaProducto, colEntradaId);

            boolean entradaCompletada = false;
            try (PreparedStatement ps = conn.prepareStatement(sqlEstado)) {
                ps.setString(1, lote);
                ps.setString(2, idProducto);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String estado = rs.getString("estado");
                        entradaCompletada = estado != null && estado.trim().equalsIgnoreCase("completado");
                    }
                }
            }

            boolean tieneSalida = false;
            if (colArticuloDetalleSalida != null) {
                String sqlSalida = """
                    SELECT 1
                    FROM articulo a
                    JOIN detalle_Entrada de ON de.`%s` = a.`%s`
                    WHERE a.`%s` = ? AND de.`%s` = ?
                      AND a.`%s` IS NOT NULL AND a.`%s` <> 0
                    LIMIT 1
                """.formatted(colDetalleEntradaId, colArticuloDetalleEntrada, colArticuloLote,
                        colDetalleEntradaProducto, colArticuloDetalleSalida, colArticuloDetalleSalida);

                try (PreparedStatement ps = conn.prepareStatement(sqlSalida)) {
                    ps.setString(1, lote);
                    ps.setString(2, idProducto);
                    try (ResultSet rs = ps.executeQuery()) {
                        tieneSalida = rs.next();
                    }
                }
            }

            return new ValidacionLoteSalida(entradaCompletada, tieneSalida);
        } catch (Exception e) {
            System.out.println("Error en validarLoteTraspasoSalida: " + e.getMessage());
            return null;
        }
    }

    private static Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new HashMap<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String limpio = nombre.trim();
                columnas.put(limpio.toLowerCase(), limpio);
            }
        }

        if (columnas.isEmpty()) {
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla.toLowerCase(), null)) {
                while (rs.next()) {
                    String nombre = rs.getString("COLUMN_NAME");
                    if (nombre == null) {
                        continue;
                    }
                    String limpio = nombre.trim();
                    columnas.put(limpio.toLowerCase(), limpio);
                }
            }
        }

        return columnas;
    }

    private static String resolverColumna(Map<String, String> columnas, String... candidatos) {
        for (String candidato : candidatos) {
            if (candidato == null) {
                continue;
            }
            String match = columnas.get(candidato.toLowerCase());
            if (match != null) {
                return match;
            }
        }
        return null;
    }

}
