package Formularios.model;

import conexion.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class modelNuevoTraspasoSalida {

    public List<String> obtenerNombresUbicaciones() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM ubicaciones ORDER BY nombre";

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

    public Optional<PreciosProducto> obtenerPreciosProducto(String idProducto) {
        String sql = """
            SELECT precioUnitario, precioIVA, precioBrutoTotal, precioTotal
            FROM detalle_Entrada
            WHERE claveProducto = ?
            ORDER BY idDetalleEntrada DESC
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal precioUnitario = obtenerDecimal(rs, "precioUnitario");
                    BigDecimal precioIva = obtenerDecimal(rs, "precioIVA");
                    BigDecimal precioBruto = obtenerDecimal(rs, "precioBrutoTotal");
                    BigDecimal precioTotal = obtenerDecimal(rs, "precioTotal");
                    return Optional.of(new PreciosProducto(precioUnitario, precioIva, precioBruto, precioTotal));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private BigDecimal obtenerDecimal(ResultSet rs, String columna) {
        try {
            BigDecimal valor = rs.getBigDecimal(columna);
            return valor != null ? valor : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public static class PreciosProducto {
        private final BigDecimal precioUnitario;
        private final BigDecimal precioIva;
        private final BigDecimal precioBruto;
        private final BigDecimal precioTotal;

        public PreciosProducto(BigDecimal precioUnitario, BigDecimal precioIva,
                               BigDecimal precioBruto, BigDecimal precioTotal) {
            this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
            this.precioIva = precioIva != null ? precioIva : BigDecimal.ZERO;
            this.precioBruto = precioBruto != null ? precioBruto : BigDecimal.ZERO;
            this.precioTotal = precioTotal != null ? precioTotal : BigDecimal.ZERO;
        }

        public BigDecimal getPrecioUnitario() {
            return precioUnitario;
        }

        public BigDecimal getPrecioIva() {
            return precioIva;
        }

        public BigDecimal getPrecioBruto() {
            return precioBruto;
        }

        public BigDecimal getPrecioTotal() {
            return precioTotal;
        }
    }
}
