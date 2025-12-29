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

    public boolean actualizarEstadoEntradas(List<String> clavesEntrada, String nuevoEstado, boolean eliminarArticulos) {
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

            if (eliminarArticulos) {
                boolean eliminado = eliminarArticulosPorEntradas(conn, clavesEntrada, colId);
                if (!eliminado) {
                    conn.rollback();
                    return false;
                }
            }

            boolean actualizado = actualizarEstado(conn, clavesEntrada, colId, colEstado, nuevoEstado);
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
