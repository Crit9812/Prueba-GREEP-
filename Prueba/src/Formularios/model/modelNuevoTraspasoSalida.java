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
        return obtenerPreciosProducto(idProducto, null, null, null);
    }

    public Optional<PreciosProducto> obtenerPreciosProducto(String idProducto, String lote,
                                                            java.time.LocalDate caducidad, String ubicacionNombre) {
        String sql = """
            SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal
            FROM detalle_Entrada de
            JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
            JOIN ubicaciones u ON u.id = a.ubicacion
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND a.caducidad = ?
              AND u.nombre = ?
            ORDER BY de.idDetalleEntrada DESC
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, lote != null ? lote : "");
            if (caducidad != null) {
                ps.setDate(3, java.sql.Date.valueOf(caducidad));
            } else {
                ps.setDate(3, null);
            }
            ps.setString(4, ubicacionNombre != null ? ubicacionNombre : "");

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

    public boolean existeLote(String lote) {
        String sql = "SELECT 1 FROM articulo WHERE lote = ? LIMIT 1";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lote);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existeLoteConCaducidad(String lote, java.time.LocalDate caducidad) {
        String sql = "SELECT 1 FROM articulo WHERE lote = ? AND caducidad = ? LIMIT 1";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lote);
            ps.setDate(2, java.sql.Date.valueOf(caducidad));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existeLoteCaducidadUbicacion(String lote, java.time.LocalDate caducidad, String ubicacionNombre) {
        String sql = """
            SELECT 1
            FROM articulo a
            JOIN ubicaciones u ON u.id = a.ubicacion
            WHERE a.lote = ? AND a.caducidad = ? AND u.nombre = ?
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lote);
            ps.setDate(2, java.sql.Date.valueOf(caducidad));
            ps.setString(3, ubicacionNombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerCantidadDisponible(String lote, java.time.LocalDate caducidad, String ubicacionNombre) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM articulo a
            JOIN ubicaciones u ON u.id = a.ubicacion
            WHERE a.lote = ? AND a.caducidad = ? AND u.nombre = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lote);
            ps.setDate(2, java.sql.Date.valueOf(caducidad));
            ps.setString(3, ubicacionNombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean existeLoteParaProducto(String lote, String idProducto) {
        String sql = """
            SELECT 1
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE a.lote = ? AND de.claveProducto = ?
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lote);
            ps.setString(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public Optional<java.time.LocalDate> obtenerCaducidadParaLoteProducto(String lote, String idProducto) {
        String sql = """
            SELECT a.caducidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE a.lote = ? AND de.claveProducto = ?
            ORDER BY a.caducidad DESC
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lote);
            ps.setString(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date caducidad = rs.getDate("caducidad");
                    if (caducidad != null) {
                        return Optional.of(caducidad.toLocalDate());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public int obtenerCantidadDisponibleProductoLoteCaducidad(String idProducto, String lote,
                                                              java.time.LocalDate caducidad) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.lote = ? AND a.caducidad = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, lote);
            ps.setDate(3, java.sql.Date.valueOf(caducidad));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean existePresentacionParaProductoLote(String idProducto, String lote, String presentacion) {
        String sql = """
            SELECT 1
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ?
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, lote);
            ps.setString(3, presentacion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existeFactorParaProductoLotePresentacion(String idProducto, String lote,
                                                            String presentacion, int factor) {
        String sql = """
            SELECT 1
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ? AND a.factor = ?
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, lote);
            ps.setString(3, presentacion);
            ps.setInt(4, factor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
