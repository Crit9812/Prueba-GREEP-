package Operaciones.compra.model;

import conexion.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class model {

    public List<String> obtenerNombresProveedores() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM proveedores ORDER BY nombre";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public String obtenerIdProveedorPorNombre(String nombreProveedor) {
        String sql = "SELECT id FROM proveedores WHERE nombre = ? LIMIT 1";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean registrarCompra(String idProveedor, String factura, String comentario, List<compra> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            Map<String, Object> valoresEntrada = new LinkedHashMap<>();

            String colProveedor = resolverColumna(columnasEntradas, "idProveedor", "id_proveedor", "proveedor", "proveedor_id");
            String colFactura = resolverColumna(columnasEntradas, "factura", "noFactura", "numeroFactura", "numero_factura");
            String colComentario = resolverColumna(columnasEntradas, "comentario", "observaciones", "nota");
            String colFecha = resolverColumna(columnasEntradas, "fecha", "fechaEntrada", "fecha_entrada", "created_at");

            if (colProveedor != null) valoresEntrada.put(colProveedor, idProveedor);
            if (colFactura != null) valoresEntrada.put(colFactura, factura);
            if (colComentario != null) valoresEntrada.put(colComentario, comentario);
            if (colFecha != null) valoresEntrada.put(colFecha, new Timestamp(System.currentTimeMillis()));

            long idEntrada = insertarRegistro(conn, "entradas", columnasEntradas, valoresEntrada);

            Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

            for (compra item : items) {
                Map<String, Object> valoresDetalle = new LinkedHashMap<>();

                String colEntrada = resolverColumna(columnasDetalle, "idEntrada", "id_entrada", "entrada_id");
                String colProducto = resolverColumna(columnasDetalle, "idProducto", "id_producto", "producto_id");
                String colCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadEntrada");
                String colPrecioEntrada = resolverColumna(columnasDetalle, "precioEntrada", "precio_entrada", "costoEntrada");
                String colPrecioIva = resolverColumna(columnasDetalle, "precioIva", "precio_iva");
                String colPrecioBruto = resolverColumna(columnasDetalle, "precioBruto", "precio_bruto");
                String colPrecioTotal = resolverColumna(columnasDetalle, "precioTotal", "precio_total");
                String colClaveAlterna = resolverColumna(columnasDetalle, "idAlterno", "claveAlterna", "clave_alterna");
                String colPresentacion = resolverColumna(columnasDetalle, "presentacion");
                String colFactor = resolverColumna(columnasDetalle, "factor");
                String colLote = resolverColumna(columnasDetalle, "lote");
                String colCaducidad = resolverColumna(columnasDetalle, "caducidad");

                if (colEntrada != null) valoresDetalle.put(colEntrada, idEntrada);
                if (colProducto != null) valoresDetalle.put(colProducto, item.getClaveProducto());
                if (colCantidad != null) valoresDetalle.put(colCantidad, item.getCantidad());
                if (colPrecioEntrada != null) valoresDetalle.put(colPrecioEntrada, item.getPrecioEntrada());
                if (colPrecioIva != null) valoresDetalle.put(colPrecioIva, item.getPrecioIva());
                if (colPrecioBruto != null) valoresDetalle.put(colPrecioBruto, item.getPrecioBruto());
                if (colPrecioTotal != null) valoresDetalle.put(colPrecioTotal, item.getPrecioTotal());
                if (colClaveAlterna != null) valoresDetalle.put(colClaveAlterna, item.getClaveAlterna());
                if (colPresentacion != null) valoresDetalle.put(colPresentacion, item.getPresentacion());
                if (colFactor != null) valoresDetalle.put(colFactor, item.getFactor());
                if (colLote != null) valoresDetalle.put(colLote, item.getLote());
                if (colCaducidad != null) valoresDetalle.put(colCaducidad, item.getCaducidad());

                insertarRegistro(conn, "detalle_Entrada", columnasDetalle, valoresDetalle);

                for (UbicacionCompra ubicacion : item.getUbicaciones()) {
                    Map<String, Object> valoresArticulo = new LinkedHashMap<>();
                    String colArticuloProducto = resolverColumna(columnasArticulo, "idProducto", "id_producto", "producto_id");
                    String colArticuloLote = resolverColumna(columnasArticulo, "lote");
                    String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
                    String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
                    String colArticuloCantidad = resolverColumna(columnasArticulo, "cantidad", "existencia");
                    String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
                    String colArticuloFactor = resolverColumna(columnasArticulo, "factor");
                    String colArticuloPrecio = resolverColumna(columnasArticulo, "precioEntrada", "precio_entrada", "costoEntrada");
                    String colArticuloClaveAlterna = resolverColumna(columnasArticulo, "idAlterno", "claveAlterna", "clave_alterna");

                    if (colArticuloProducto != null) valoresArticulo.put(colArticuloProducto, item.getClaveProducto());
                    if (colArticuloLote != null) valoresArticulo.put(colArticuloLote, item.getLote());
                    if (colArticuloCaducidad != null) valoresArticulo.put(colArticuloCaducidad, item.getCaducidad());
                    if (colArticuloUbicacion != null) valoresArticulo.put(colArticuloUbicacion, ubicacion.getUbicacion());
                    if (colArticuloCantidad != null) valoresArticulo.put(colArticuloCantidad, ubicacion.getCantidad());
                    if (colArticuloPresentacion != null) valoresArticulo.put(colArticuloPresentacion, item.getPresentacion());
                    if (colArticuloFactor != null) valoresArticulo.put(colArticuloFactor, item.getFactor());
                    if (colArticuloPrecio != null) valoresArticulo.put(colArticuloPrecio, item.getPrecioEntrada());
                    if (colArticuloClaveAlterna != null) valoresArticulo.put(colArticuloClaveAlterna, item.getClaveAlterna());

                    insertarRegistro(conn, "articulo", columnasArticulo, valoresArticulo);
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new java.util.HashMap<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                columnas.put(nombre.toLowerCase(), nombre);
            }
        }

        if (columnas.isEmpty()) {
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla.toLowerCase(), null)) {
                while (rs.next()) {
                    String nombre = rs.getString("COLUMN_NAME");
                    columnas.put(nombre.toLowerCase(), nombre);
                }
            }
        }

        return columnas;
    }

    private String resolverColumna(Map<String, String> columnas, String... candidatos) {
        for (String candidato : candidatos) {
            if (candidato == null) continue;
            String match = columnas.get(candidato.toLowerCase());
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private long insertarRegistro(Connection conn, String tabla, Map<String, String> columnas, Map<String, Object> valores)
            throws SQLException {
        if (valores.isEmpty()) {
            throw new SQLException("No hay valores para insertar en " + tabla);
        }

        String columnasSql = String.join(", ", valores.keySet());
        String placeholders = String.join(", ", java.util.Collections.nCopies(valores.size(), "?"));

        String sql = "INSERT INTO " + tabla + " (" + columnasSql + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            for (Object value : valores.values()) {
                ps.setObject(index++, value);
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT LAST_INSERT_ID()")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        return 0;
    }
}
