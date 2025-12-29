package Operaciones.traspasoEntrada.model;

import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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
