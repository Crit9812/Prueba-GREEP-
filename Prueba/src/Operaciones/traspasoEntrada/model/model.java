package Operaciones.traspasoEntrada.model;

import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class model {

    // Clase interna para representar los detalles de una entrada
    public static class DetalleEntrada {
        private String claveProducto;
        private String producto;
        private String cantidad;
        private String precioUnitario;
        private String precioTotal;

        public DetalleEntrada(String claveProducto, String producto, String cantidad, String precioUnitario, String precioTotal) {
            this.claveProducto = claveProducto;
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.precioTotal = precioTotal;
        }

        public String getClaveProducto() { return claveProducto; }
        public String getProducto() { return producto; }
        public String getCantidad() { return cantidad; }
        public String getPrecioUnitario() { return precioUnitario; }
        public String getPrecioTotal() { return precioTotal; }
    }

    public String obtenerNombreProducto(String claveProducto) {
        String nombreProducto = "";
        String marca = "";
        String presentacion = "";
        String compuesto = "";

        if (claveProducto == null || claveProducto.trim().isEmpty()) {
            return claveProducto; // Devolver la clave si está vacía
        }

        try (Connection conn = new Conexion().conectar()) {
            // Obtener columnas de la tabla productos
            Map<String, String> columnasProductos = obtenerColumnas(conn, "productos");

            // Buscar las columnas necesarias
            String colClaveProducto = resolverColumna(columnasProductos, "id", "claveProducto", "clave_producto", "producto_id");
            String colNombre = resolverColumna(columnasProductos, "nombre", "nombreProducto", "producto_nombre", "descripcion");
            String colMarca = resolverColumna(columnasProductos, "marca", "idMarca", "marca_id");
            String colPresentacion = resolverColumna(columnasProductos, "presentacion", "unidadMedida", "unidad_medida", "presentation");

            // Verificar que las columnas necesarias existen
            if (colClaveProducto == null || colNombre == null) {
                System.err.println("No se pudieron encontrar las columnas necesarias en productos");
                return claveProducto; // Devolver la clave si no encuentra columnas
            }

            String sql = "SELECT " +
                    "`" + colNombre + "` AS nombre, " +
                    (colMarca != null ? "`" + colMarca + "` AS marca, " : "NULL AS marca, ") +
                    (colPresentacion != null ? "`" + colPresentacion + "` AS presentacion " : "NULL AS presentacion ") +
                    "FROM `productos` " +
                    "WHERE `" + colClaveProducto + "` = ? " +
                    "LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, claveProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nombreProducto = formato(rs.getObject("nombre"));
                        marca = formato(rs.getObject("marca"));
                        presentacion = formato(rs.getObject("presentacion"));

                        // Construir el compuesto
                        StringBuilder sb = new StringBuilder();
                        sb.append(nombreProducto);

                        if (marca != null && !marca.isEmpty()) {
                            sb.append(" - ").append(marca);
                        }

                        if (presentacion != null && !presentacion.isEmpty()) {
                            sb.append(" (").append(presentacion).append(")");
                        }

                        compuesto = sb.toString();

                    } else {
                        compuesto = claveProducto; // Devolver la clave como nombre
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error al obtener información del producto '" + claveProducto + "': " + e.getMessage());
            e.printStackTrace();
            // En caso de error, devolver la clave del producto
            compuesto = claveProducto;
        }

        return compuesto.isEmpty() ? claveProducto : compuesto;
    }

    public List<String> obtenerNombresUbicaciones() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM ubicaciones WHERE LOWER(TRIM(COALESCE(estado, ''))) = 'activo' ORDER BY nombre";

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

    public ObservableList<traspasoEntrada> obtenerPendientes() {
        ObservableList<traspasoEntrada> lista = FXCollections.observableArrayList();

        try (Connection conn = new Conexion().conectar()) {
            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            Map<String, String> columnasSucursales = obtenerColumnas(conn, "sucursales");

            String colId = resolverColumna(columnasEntradas, "id", "claveEntrada", "idEntrada", "entrada_id");
            String colFecha = resolverColumna(columnasEntradas, "fechaEntrada", "fecha", "fecha_entrada", "created_at");
            String colHora = resolverColumna(columnasEntradas, "horaEntrada", "hora", "hora_entrada");
            String colTotal = resolverColumna(columnasEntradas, "precioTotalEntrada", "precioTotal", "precio_total", "total", "totalEntrada");
            String colTipo = resolverColumna(columnasEntradas, "tipoEntrada", "tipo", "tipo_entrada");
            String colEstado = resolverColumna(columnasEntradas, "Estado", "estado");
            String colSucursal = resolverColumna(columnasEntradas, "idRemitente", "idSucursal", "sucursal", "sucursal_id", "id_sucursal", "remitente");

            if (colTipo == null || colEstado == null) {
                return lista;
            }

            String colSucursalId = resolverColumna(columnasSucursales, "id", "idSucursal", "sucursal_id", "id_sucursal");
            String colSucursalNombre = resolverColumna(columnasSucursales, "nombre", "nombreSucursal", "sucursal");
            boolean puedeUnirSucursal = colSucursal != null && colSucursalId != null && colSucursalNombre != null;

            String selectClave = colId != null ? "e.`" + colId + "` AS clave" : "NULL AS clave";
            String selectFecha = colFecha != null ? "e.`" + colFecha + "` AS fecha" : "NULL AS fecha";
            String selectHora = colHora != null ? "e.`" + colHora + "` AS hora" : "NULL AS hora";
            String selectTotal = colTotal != null ? "e.`" + colTotal + "` AS total" : "NULL AS total";
            String selectSucursal = puedeUnirSucursal
                    ? "s.`" + colSucursalNombre + "` AS sucursal"
                    : "NULL AS sucursal";

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ")
                    .append(selectClave).append(", ")
                    .append(selectFecha).append(", ")
                    .append(selectHora).append(", ")
                    .append(selectTotal).append(", ")
                    .append(selectSucursal)
                    .append(" FROM entradas e ");

            if (puedeUnirSucursal) {
                sql.append("LEFT JOIN sucursales s ON e.`")
                        .append(colSucursal)
                        .append("` = s.`")
                        .append(colSucursalId)
                        .append("` ");
            }

            sql.append("WHERE LOWER(e.`")
                    .append(colTipo)
                    .append("`) = ? AND LOWER(e.`")
                    .append(colEstado)
                    .append("`) = ? ")
                    .append("ORDER BY ")
                    .append(colFecha != null ? "e.`" + colFecha + "`" : "clave")
                    .append(" DESC");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setString(1, "traspaso");
                ps.setString(2, "pendiente");

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String clave = formato(rs.getObject("clave"));
                        String fecha = formato(rs.getObject("fecha"));
                        String hora = formato(rs.getObject("hora"));
                        String total = formato(rs.getObject("total"));
                        String sucursal = formato(rs.getObject("sucursal"));

                        lista.add(new traspasoEntrada(clave, fecha, hora, total, sucursal));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    // Método para obtener detalles de una entrada específica
    public List<DetalleEntrada> obtenerDetallesEntrada(String claveEntrada) {
        List<DetalleEntrada> detalles = new ArrayList<>();

        if (claveEntrada == null || claveEntrada.trim().isEmpty()) {
            return detalles;
        }

        try (Connection conn = new Conexion().conectar()) {
            // Obtener columnas de detalle_Entrada
            Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");

            // Buscar las columnas necesarias
            String colClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
            String colClaveProducto = resolverColumna(columnasDetalle, "claveProducto", "idProducto", "producto_id", "clave_producto");
            String colCantidad = resolverColumna(columnasDetalle, "cantidad", "qty", "quantity");
            String colPrecioUnitario = resolverColumna(columnasDetalle, "precioUnitario", "unitario", "precio_unitario", "unit_price");
            String colPrecioTotal = resolverColumna(columnasDetalle, "precioTotal", "precio_total", "total", "precio_final", "final_price");

            // Verificar que todas las columnas necesarias existen
            if (colClaveEntrada == null || colClaveProducto == null || colCantidad == null ||
                    colPrecioUnitario == null || colPrecioTotal == null) {
                System.err.println("No se pudieron encontrar todas las columnas necesarias en detalle_Entrada");
                return detalles;
            }

            String sql = "SELECT " +
                    "`" + colClaveProducto + "` AS claveProducto, " +
                    "`" + colCantidad + "` AS cantidad, " +
                    "`" + colPrecioUnitario + "` AS precioUnitario, " +
                    "`" + colPrecioTotal + "` AS precioTotal " +
                    "FROM `detalle_Entrada` " +
                    "WHERE `" + colClaveEntrada + "` = ? " +
                    "ORDER BY `" + colClaveProducto + "`";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, claveEntrada);

                try (ResultSet rs = ps.executeQuery()) {
                    int contador = 0;
                    while (rs.next()) {
                        String claveProd = formato(rs.getObject("claveProducto"));
                        String cantidad = formato(rs.getObject("cantidad"));
                        String precioUnitario = formato(rs.getObject("precioUnitario"));
                        String precioTotal = formato(rs.getObject("precioTotal"));

                        // DEBUG: Imprimir lo que se obtuvo de la base de datos
                        System.out.println("Detalle " + (++contador) + " para entrada " + claveEntrada +
                                ": ClaveProducto=" + claveProd +
                                ", Cantidad=" + cantidad +
                                ", PrecioUnitario=" + precioUnitario +
                                ", PrecioTotal=" + precioTotal);

                        // Obtener el nombre del producto usando el método existente
                        String nombreProducto = obtenerNombreProducto(claveProd);


                        // Ahora sí, crear el DetalleEntrada con los 5 parámetros
                        detalles.add(new DetalleEntrada(claveProd, nombreProducto, cantidad, precioUnitario, precioTotal));
                    }

                }
            }

        } catch (Exception e) {
            System.err.println("Error al obtener detalles de la entrada: " + e.getMessage());
            e.printStackTrace();
        }

        return detalles;
    }

    public boolean actualizarEstadoEntradas(List<String> clavesEntrada, String nuevoEstadoEntrada, String nuevoEstadoArticulos) {
        if (clavesEntrada == null || clavesEntrada.isEmpty()) {
            return false;
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            String colId = resolverColumna(columnasEntradas, "id", "claveEntrada", "idEntrada", "entrada_id");
            String colEstado = resolverColumna(columnasEntradas, "Estado", "estado");

            if (colId == null || colEstado == null) {
                conn.rollback();
                return false;
            }

            boolean actualizadoArticulos = actualizarEstadoArticulosPorEntradas(
                    conn,
                    clavesEntrada,
                    nuevoEstadoArticulos
            );
            if (!actualizadoArticulos) {
                conn.rollback();
                return false;
            }

            boolean actualizado = actualizarEstado(conn, clavesEntrada, colId, colEstado, nuevoEstadoEntrada);
            if (!actualizado) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean actualizarEstado(Connection conn, List<String> clavesEntrada, String colId, String colEstado, String nuevoEstado) throws SQLException {
        List<String> claves = filtrarClaves(clavesEntrada);
        if (claves.isEmpty()) {
            return false;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(claves.size(), "?"));
        String sql = "UPDATE entradas SET `" + colEstado + "` = ? WHERE `" + colId + "` IN (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            int index = 2;
            for (String clave : claves) {
                ps.setString(index++, clave);
            }
            ps.executeUpdate();
        }

        return true;
    }

    private boolean eliminarArticulosPorEntradas(Connection conn, List<String> clavesEntrada, String colEntrada) throws SQLException {
        List<String> claves = filtrarClaves(clavesEntrada);
        if (claves.isEmpty()) {
            return false;
        }

        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

        String colDetalleId = resolverColumna(columnasDetalle, "id", "idDetalleEntrada", "id_detalle_entrada", "detalle_entrada_id", "detalleEntrada");
        String colDetalleEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colArticuloDetalle = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");

        if (colDetalleId == null || colDetalleEntrada == null || colArticuloDetalle == null) {
            return false;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(claves.size(), "?"));
        String sql = "DELETE a FROM articulo a " +
                "JOIN detalle_Entrada d ON a.`" + colArticuloDetalle + "` = d.`" + colDetalleId + "` " +
                "WHERE d.`" + colDetalleEntrada + "` IN (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (String clave : claves) {
                ps.setString(index++, clave);
            }
            ps.executeUpdate();
        }

        return true;
    }

    private boolean actualizarEstadoArticulosPorEntradas(Connection conn, List<String> clavesEntrada, String nuevoEstado)
            throws SQLException {
        List<String> claves = filtrarClaves(clavesEntrada);
        if (claves.isEmpty()) {
            return false;
        }

        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

        String colDetalleId = resolverColumna(columnasDetalle, "id", "idDetalleEntrada", "id_detalle_entrada",
                "detalle_entrada_id", "detalleEntrada");
        String colDetalleEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colArticuloDetalle = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

        if (colDetalleId == null || colDetalleEntrada == null || colArticuloDetalle == null || colArticuloEstado == null) {
            return false;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(claves.size(), "?"));
        String sql = "UPDATE articulo a " +
                "JOIN detalle_Entrada d ON a.`" + colArticuloDetalle + "` = d.`" + colDetalleId + "` " +
                "SET a.`" + colArticuloEstado + "` = ? " +
                "WHERE d.`" + colDetalleEntrada + "` IN (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            ps.setString(index++, nuevoEstado);
            for (String clave : claves) {
                ps.setString(index++, clave);
            }
            ps.executeUpdate();
        }

        return true;
    }

    private List<String> filtrarClaves(List<String> clavesEntrada) {
        List<String> claves = new ArrayList<>();
        for (String clave : clavesEntrada) {
            if (clave != null && !clave.isBlank()) {
                claves.add(clave.trim());
            }
        }
        return claves;
    }

    private String formato(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new HashMap<>();
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String limpio = nombre.trim();
                columnas.put(limpio.toLowerCase(), limpio);
            }
        }
        return columnas;
    }

    private String resolverColumna(Map<String, String> columnas, String... candidatos) {
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
