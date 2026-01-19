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

    public static class ValidacionDisponibilidadSalida {
        private final boolean entradaCompletada;
        private final int disponiblesSinSalida;

        public ValidacionDisponibilidadSalida(boolean entradaCompletada, int disponiblesSinSalida) {
            this.entradaCompletada = entradaCompletada;
            this.disponiblesSinSalida = disponiblesSinSalida;
        }

        public boolean isEntradaCompletada() {
            return entradaCompletada;
        }

        public int getDisponiblesSinSalida() {
            return disponiblesSinSalida;
        }
    }

    public static ValidacionDisponibilidadSalida validarEntradaYDisponibilidadLoteProducto(Connection conn,
                                                                                           String lote,
                                                                                           String idProducto) {
        if (conn == null || lote == null || idProducto == null) {
            return new ValidacionDisponibilidadSalida(false, 0);
        }

        try {
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");

            if (colArticuloDetalleEntrada == null || colArticuloDetalleSalida == null || colArticuloLote == null
                    || colDetalleEntradaId == null || colDetalleEntradaProducto == null) {
                return new ValidacionDisponibilidadSalida(false, 0);
            }

            String disponibleCond = "(a.`%s` IS NULL OR a.`%s` = 0)".formatted(
                    colArticuloDetalleSalida,
                    colArticuloDetalleSalida
            );
            if (colArticuloEstado != null) {
                disponibleCond = disponibleCond + " AND LOWER(a.`" + colArticuloEstado + "`) = ?";
            }

            String sql = """
                SELECT
                    COUNT(*) AS total_articulos,
                    SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS disponibles
                FROM articulo a
                JOIN detalle_Entrada de ON de.`%s` = a.`%s`
                WHERE a.`%s` = ? AND de.`%s` = ?
            """.formatted(
                    disponibleCond,
                    colDetalleEntradaId,
                    colArticuloDetalleEntrada,
                    colArticuloLote,
                    colDetalleEntradaProducto
            );

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                if (colArticuloEstado != null) {
                    ps.setString(index++, "disponible");
                }
                ps.setString(index++, lote);
                ps.setString(index, idProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int completados = rs.getInt("total_articulos");
                        int disponibles = rs.getInt("disponibles");
                        return new ValidacionDisponibilidadSalida(completados > 0, disponibles);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en validarEntradaYDisponibilidadLoteProducto: " + e.getMessage());
        }

        return new ValidacionDisponibilidadSalida(false, 0);
    }

    public static int contarDisponiblesSinSalidaPorLoteProducto(Connection conn, String lote, String idProducto) {
        if (conn == null || lote == null || idProducto == null) {
            return 0;
        }

        try {
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");

            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");

            if (colArticuloDetalleEntrada == null || colArticuloDetalleSalida == null || colArticuloLote == null
                    || colDetalleEntradaId == null || colDetalleEntradaProducto == null) {
                return 0;
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) AS total ")
                    .append("FROM articulo a ")
                    .append("JOIN detalle_Entrada de ON de.`").append(colDetalleEntradaId)
                    .append("` = a.`").append(colArticuloDetalleEntrada).append("` ")
                    .append("WHERE a.`").append(colArticuloLote).append("` = ? AND de.`")
                    .append(colDetalleEntradaProducto).append("` = ? ")
                    .append("AND (a.`").append(colArticuloDetalleSalida).append("` IS NULL OR a.`")
                    .append(colArticuloDetalleSalida).append("` = 0)");
            if (colArticuloEstado != null) {
                sql.append(" AND LOWER(a.`").append(colArticuloEstado).append("`) = ?");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int index = 1;
                ps.setString(index++, lote);
                ps.setString(index++, idProducto);
                if (colArticuloEstado != null) {
                    ps.setString(index, "disponible");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en contarDisponiblesSinSalidaPorLoteProducto: " + e.getMessage());
        }

        return 0;
    }

    public static int contarDisponiblesSinSalidaPorLoteProductoCaducidad(Connection conn, String lote, String idProducto,
                                                                         java.time.LocalDate caducidad) {
        if (conn == null || lote == null || idProducto == null) {
            return 0;
        }

        try {
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");

            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");
            String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");

            if (colArticuloDetalleEntrada == null || colArticuloDetalleSalida == null || colArticuloLote == null
                    || colArticuloCaducidad == null || colDetalleEntradaId == null || colDetalleEntradaProducto == null) {
                return 0;
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) AS total ")
                    .append("FROM articulo a ")
                    .append("JOIN detalle_Entrada de ON de.`").append(colDetalleEntradaId)
                    .append("` = a.`").append(colArticuloDetalleEntrada).append("` ")
                    .append("WHERE de.`").append(colDetalleEntradaProducto).append("` = ? AND a.`")
                    .append(colArticuloLote).append("` = ? ");
            if (caducidad == null) {
                sql.append("AND a.`").append(colArticuloCaducidad).append("` IS NULL ");
            } else {
                sql.append("AND a.`").append(colArticuloCaducidad).append("` = ? ");
            }
            sql.append("AND (a.`").append(colArticuloDetalleSalida).append("` IS NULL OR a.`")
                    .append(colArticuloDetalleSalida).append("` = 0)");
            if (colArticuloEstado != null) {
                sql.append(" AND LOWER(a.`").append(colArticuloEstado).append("`) = ?");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int index = 1;
                ps.setString(index++, idProducto);
                ps.setString(index++, lote);
                if (caducidad != null) {
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                }
                if (colArticuloEstado != null) {
                    ps.setString(index, "disponible");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en contarDisponiblesSinSalidaPorLoteProductoCaducidad: " + e.getMessage());
        }

        return 0;
    }

    public static int contarDisponiblesSinSalidaPorLoteProductoPresentacionFactor(Connection conn, String lote,
                                                                                  String idProducto,
                                                                                  String presentacion, int factor) {
        if (conn == null || lote == null || idProducto == null || presentacion == null) {
            return 0;
        }

        try {
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");

            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");
            String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
            String colArticuloFactor = resolverColumna(columnasArticulo, "factor");
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");

            if (colArticuloDetalleEntrada == null || colArticuloDetalleSalida == null || colArticuloLote == null
                    || colArticuloPresentacion == null || colArticuloFactor == null
                    || colDetalleEntradaId == null || colDetalleEntradaProducto == null) {
                return 0;
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) AS total ")
                    .append("FROM articulo a ")
                    .append("JOIN detalle_Entrada de ON de.`").append(colDetalleEntradaId)
                    .append("` = a.`").append(colArticuloDetalleEntrada).append("` ")
                    .append("WHERE a.`").append(colArticuloLote).append("` = ? ")
                    .append("AND de.`").append(colDetalleEntradaProducto).append("` = ? ")
                    .append("AND a.`").append(colArticuloPresentacion).append("` = ? ")
                    .append("AND a.`").append(colArticuloFactor).append("` = ? ")
                    .append("AND (a.`").append(colArticuloDetalleSalida).append("` IS NULL OR a.`")
                    .append(colArticuloDetalleSalida).append("` = 0)");
            if (colArticuloEstado != null) {
                sql.append(" AND LOWER(a.`").append(colArticuloEstado).append("`) = ?");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int index = 1;
                ps.setString(index++, lote);
                ps.setString(index++, idProducto);
                ps.setString(index++, presentacion);
                ps.setInt(index++, factor);
                if (colArticuloEstado != null) {
                    ps.setString(index, "disponible");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en contarDisponiblesSinSalidaPorLoteProductoPresentacionFactor: "
                    + e.getMessage());
        }

        return 0;
    }

    public static int contarDisponiblesSinSalidaPorLoteCaducidadUbicacion(Connection conn, String lote,
                                                                          java.time.LocalDate caducidad,
                                                                          String ubicacionNombre) {
        if (conn == null || lote == null || ubicacionNombre == null) {
            return 0;
        }

        try {
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasUbicaciones = obtenerColumnas(conn, "ubicaciones");

            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");
            String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
            String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

            String colUbicacionId = resolverColumna(columnasUbicaciones, "id", "idUbicacion", "ubicacion_id",
                    "id_ubicacion");
            String colUbicacionNombre = resolverColumna(columnasUbicaciones, "nombre", "nombreUbicacion", "ubicacion");

            if (colArticuloDetalleSalida == null || colArticuloLote == null || colArticuloCaducidad == null
                    || colArticuloUbicacion == null || colUbicacionId == null || colUbicacionNombre == null) {
                return 0;
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) AS total ")
                    .append("FROM articulo a ")
                    .append("JOIN ubicaciones u ON u.`").append(colUbicacionId)
                    .append("` = a.`").append(colArticuloUbicacion).append("` ")
                    .append("WHERE a.`").append(colArticuloLote).append("` = ? ");
            if (caducidad == null) {
                sql.append("AND a.`").append(colArticuloCaducidad).append("` IS NULL ");
            } else {
                sql.append("AND a.`").append(colArticuloCaducidad).append("` = ? ");
            }
            sql.append("AND u.`").append(colUbicacionNombre).append("` = ? ")
                    .append("AND (a.`").append(colArticuloDetalleSalida).append("` IS NULL OR a.`")
                    .append(colArticuloDetalleSalida).append("` = 0)");
            if (colArticuloEstado != null) {
                sql.append(" AND LOWER(a.`").append(colArticuloEstado).append("`) = ?");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int index = 1;
                ps.setString(index++, lote);
                if (caducidad != null) {
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                }
                ps.setString(index++, ubicacionNombre);
                if (colArticuloEstado != null) {
                    ps.setString(index, "disponible");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en contarDisponiblesSinSalidaPorLoteCaducidadUbicacion: " + e.getMessage());
        }

        return 0;
    }

    public static int contarDisponiblesSinSalidaPorProductoLoteCaducidadUbicacion(Connection conn, String idProducto,
                                                                                  String lote,
                                                                                  java.time.LocalDate caducidad,
                                                                                  String ubicacionNombre) {
        if (conn == null || idProducto == null || lote == null || ubicacionNombre == null) {
            return 0;
        }

        try {
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasUbicaciones = obtenerColumnas(conn, "ubicaciones");

            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");
            String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
            String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");

            String colUbicacionId = resolverColumna(columnasUbicaciones, "id", "idUbicacion", "ubicacion_id",
                    "id_ubicacion");
            String colUbicacionNombre = resolverColumna(columnasUbicaciones, "nombre", "nombreUbicacion", "ubicacion");
            String colUbicacionEstado = resolverColumna(columnasUbicaciones, "estado", "Estado");

            if (colArticuloDetalleEntrada == null || colArticuloDetalleSalida == null || colArticuloLote == null
                    || colArticuloCaducidad == null || colArticuloUbicacion == null || colDetalleEntradaId == null
                    || colDetalleEntradaProducto == null || colUbicacionId == null || colUbicacionNombre == null) {
                return 0;
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) AS total ")
                    .append("FROM articulo a ")
                    .append("JOIN detalle_Entrada de ON de.`").append(colDetalleEntradaId)
                    .append("` = a.`").append(colArticuloDetalleEntrada).append("` ")
                    .append("JOIN ubicaciones u ON u.`").append(colUbicacionId)
                    .append("` = a.`").append(colArticuloUbicacion).append("` ")
                    .append("WHERE de.`").append(colDetalleEntradaProducto).append("` = ? ")
                    .append("AND a.`").append(colArticuloLote).append("` = ? ");
            if (caducidad == null) {
                sql.append("AND a.`").append(colArticuloCaducidad).append("` IS NULL ");
            } else {
                sql.append("AND a.`").append(colArticuloCaducidad).append("` = ? ");
            }
            sql.append("AND u.`").append(colUbicacionNombre).append("` = ? ")
                    .append("AND (a.`").append(colArticuloDetalleSalida).append("` IS NULL OR a.`")
                    .append(colArticuloDetalleSalida).append("` = 0)");
            if (colArticuloEstado != null) {
                sql.append(" AND LOWER(a.`").append(colArticuloEstado).append("`) = ?");
            }
            if (colUbicacionEstado != null) {
                sql.append(" AND LOWER(u.`").append(colUbicacionEstado).append("`) = ?");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int index = 1;
                ps.setString(index++, idProducto);
                ps.setString(index++, lote);
                if (caducidad != null) {
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                }
                ps.setString(index++, ubicacionNombre);
                if (colArticuloEstado != null) {
                    ps.setString(index++, "disponible");
                }
                if (colUbicacionEstado != null) {
                    ps.setString(index, "activo");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en contarDisponiblesSinSalidaPorProductoLoteCaducidadUbicacion: "
                    + e.getMessage());
        }

        return 0;
    }

    public static int contarDisponiblesSinSalidaDetalle(Connection conn, String idProducto, String lote,
                                                        java.time.LocalDate caducidad, String presentacion, int factor,
                                                        String ubicacionNombre) {
        if (conn == null || idProducto == null || lote == null || presentacion == null || ubicacionNombre == null) {
            return 0;
        }

        try {
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasUbicaciones = obtenerColumnas(conn, "ubicaciones");

            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");
            String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
            String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
            String colArticuloFactor = resolverColumna(columnasArticulo, "factor");
            String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");

            String colUbicacionId = resolverColumna(columnasUbicaciones, "id", "idUbicacion", "ubicacion_id",
                    "id_ubicacion");
            String colUbicacionNombre = resolverColumna(columnasUbicaciones, "nombre", "nombreUbicacion", "ubicacion");

            if (colArticuloDetalleEntrada == null || colArticuloDetalleSalida == null || colArticuloLote == null
                    || colArticuloCaducidad == null || colArticuloPresentacion == null || colArticuloFactor == null
                    || colArticuloUbicacion == null || colDetalleEntradaId == null || colDetalleEntradaProducto == null
                    || colUbicacionId == null || colUbicacionNombre == null) {
                return 0;
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) AS total ")
                    .append("FROM articulo a ")
                    .append("JOIN detalle_Entrada de ON de.`").append(colDetalleEntradaId)
                    .append("` = a.`").append(colArticuloDetalleEntrada).append("` ")
                    .append("JOIN ubicaciones u ON u.`").append(colUbicacionId)
                    .append("` = a.`").append(colArticuloUbicacion).append("` ")
                    .append("WHERE de.`").append(colDetalleEntradaProducto).append("` = ? ")
                    .append("AND a.`").append(colArticuloLote).append("` = ? ")
                    .append("AND a.`").append(colArticuloPresentacion).append("` = ? ")
                    .append("AND a.`").append(colArticuloFactor).append("` = ? ")
                    .append("AND u.`").append(colUbicacionNombre).append("` = ? ")
                    .append("AND (a.`").append(colArticuloDetalleSalida).append("` IS NULL OR a.`")
                    .append(colArticuloDetalleSalida).append("` = 0)");
            if (caducidad == null) {
                sql.append(" AND a.`").append(colArticuloCaducidad).append("` IS NULL");
            } else {
                sql.append(" AND a.`").append(colArticuloCaducidad).append("` = ?");
            }
            if (colArticuloEstado != null) {
                sql.append(" AND LOWER(a.`").append(colArticuloEstado).append("`) = ?");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int index = 1;
                ps.setString(index++, idProducto);
                ps.setString(index++, lote);
                ps.setString(index++, presentacion);
                ps.setInt(index++, factor);
                ps.setString(index++, ubicacionNombre);
                if (caducidad != null) {
                    ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                }
                if (colArticuloEstado != null) {
                    ps.setString(index, "disponible");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en contarDisponiblesSinSalidaDetalle: " + e.getMessage());
        }

        return 0;
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
                String nombreLimpio = nombre.trim();
                columnas.put(nombreLimpio.toLowerCase(), nombreLimpio);
            }
        }

        return columnas;
    }

    private static String resolverColumna(Map<String, String> columnas, String... candidatos) {
        if (columnas == null || candidatos == null) {
            return null;
        }
        for (String candidato : candidatos) {
            if (candidato == null) {
                continue;
            }
            String key = candidato.trim().toLowerCase();
            if (columnas.containsKey(key)) {
                return columnas.get(key);
            }
        }
        return null;
    }

}
