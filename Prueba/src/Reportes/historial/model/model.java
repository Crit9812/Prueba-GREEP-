package Reportes.historial.model;

import conexion.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class model {

    public List<MovimientoFactura> obtenerMovimientos() {
        String sql = "SELECT claveMovimiento, fecha, hora, tipoMovimiento, total, usuario, externo, facturaExterna, estado " +
                "FROM (" +
                " SELECT CAST(s.idSalida AS CHAR) AS claveMovimiento, CAST(s.fechaSalida AS CHAR) AS fecha, CAST(s.horaSalida AS CHAR) AS hora, " +
                "        s.tipoSalida AS tipoMovimiento, s.precioTotalSalida AS total, " +
                "        CONCAT_WS(' ', u.nombreUsuario, u.apellidoPUsuario, u.apellidoMUsuario) AS usuario, " +
                "        COALESCE(c.Nombre, su.nombre, s.idDestinatario) AS externo, " +
                "        CAST(s.noFactura AS CHAR) AS facturaExterna, s.Estado AS estado " +
                " FROM salidas s " +
                " LEFT JOIN usuarios u ON u.idUsuario = s.claveUsuarioSalida " +
                " LEFT JOIN clientes c ON CAST(c.id AS CHAR) = CAST(s.idDestinatario AS CHAR) " +
                " LEFT JOIN sucursales su ON CAST(su.id AS CHAR) = CAST(s.idDestinatario AS CHAR) " +
                " UNION ALL " +
                " SELECT CAST(e.idEntrada AS CHAR) AS claveMovimiento, CAST(e.fechaEntrada AS CHAR) AS fecha, CAST(e.horaEntrada AS CHAR) AS hora, " +
                "        e.tipoEntrada AS tipoMovimiento, e.precioTotalEntrada AS total, " +
                "        CONCAT_WS(' ', u.nombreUsuario, u.apellidoPUsuario, u.apellidoMUsuario) AS usuario, " +
                "        COALESCE(p.Nombre, e.idRemitente) AS externo, " +
                "        CAST(e.noFactura AS CHAR) AS facturaExterna, e.Estado AS estado " +
                " FROM entradas e " +
                " LEFT JOIN usuarios u ON u.idUsuario = e.claveUsuarioEntrada " +
                " LEFT JOIN proveedores p ON CAST(p.id AS CHAR) = CAST(e.idRemitente AS CHAR) " +
                " UNION ALL " +
                " SELECT a.idAjuste AS claveMovimiento, a.fechaAjuste AS fecha, CAST(a.horaAjuste AS CHAR) AS hora, " +
                "        'ajuste' AS tipoMovimiento, a.precioTotal AS total, " +
                "        CONCAT_WS(' ', u.nombreUsuario, u.apellidoPUsuario, u.apellidoMUsuario) AS usuario, " +
                "        'Inventario' AS externo, a.idAjuste AS facturaExterna, a.estado AS estado " +
                " FROM ajuste_inventario a " +
                " LEFT JOIN usuarios u ON u.idUsuario = a.idUsuario " +
                ") movimientos " +
                "ORDER BY fecha DESC, hora DESC";

        List<MovimientoFactura> movimientos = new ArrayList<>();
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                movimientos.add(new MovimientoFactura(
                        valor(rs.getString("claveMovimiento")),
                        valor(rs.getString("fecha")),
                        valor(rs.getString("hora")),
                        valor(rs.getString("tipoMovimiento")),
                        formatearTotal(rs.getBigDecimal("total")),
                        valor(rs.getString("usuario")),
                        valor(rs.getString("externo")),
                        valor(rs.getString("facturaExterna")),
                        valor(rs.getString("estado"))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return movimientos;
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    private String formatearTotal(BigDecimal total) {
        if (total == null) return "$0.00";
        return String.format("$%,.2f", total.doubleValue());
    }
}
