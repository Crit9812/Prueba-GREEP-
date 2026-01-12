package Formularios.model;

import Compartido.model.DAO.GenericDAO;
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
            JOIN entradas e ON e.idEntrada = de.claveEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND a.caducidad = ?
              AND u.nombre = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY e.fechaEntrada DESC, e.horaEntrada DESC, de.idDetalleEntrada DESC LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote != null ? lote : "");
            if (caducidad != null) {
                ps.setDate(index++, java.sql.Date.valueOf(caducidad));
            } else {
                ps.setDate(index++, null);
            }
            ps.setString(index++, ubicacionNombre != null ? ubicacionNombre : "");
            index = agregarParametroEstado(ps, conn, index);

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

    public Optional<PreciosProducto> obtenerPreciosProductoPorLoteCaducidad(String idProducto, String lote,
                                                                            java.time.LocalDate caducidad) {
        String sql = """
            SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal
            FROM detalle_Entrada de
            JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
            JOIN entradas e ON e.idEntrada = de.claveEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND a.caducidad = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY e.fechaEntrada DESC, e.horaEntrada DESC, de.idDetalleEntrada DESC LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote != null ? lote : "");
            if (caducidad != null) {
                ps.setDate(index++, java.sql.Date.valueOf(caducidad));
            } else {
                ps.setDate(index++, null);
            }
            index = agregarParametroEstado(ps, conn, index);

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

    public Optional<PreciosProducto> obtenerPreciosProductoUltimaEntrada(String idProducto) {
        String sql = """
            SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal
            FROM detalle_Entrada de
            JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
            JOIN entradas e ON e.idEntrada = de.claveEntrada
            WHERE de.claveProducto = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY e.fechaEntrada DESC, e.horaEntrada DESC, de.idDetalleEntrada DESC LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            index = agregarParametroEstado(ps, conn, index);

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
        String sql = "SELECT 1 FROM articulo WHERE lote = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, lote);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existeLoteConCaducidad(String lote, java.time.LocalDate caducidad) {
        String sql = "SELECT 1 FROM articulo WHERE lote = ? AND caducidad = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, lote);
            ps.setDate(index++, java.sql.Date.valueOf(caducidad));
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existeLoteCaducidadUbicacion(String lote, java.time.LocalDate caducidad, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            int disponibles = GenericDAO.contarDisponiblesSinSalidaPorLoteCaducidadUbicacion(
                    conn, lote, caducidad, ubicacionNombre);
            return disponibles > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerCantidadDisponible(String lote, java.time.LocalDate caducidad, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorLoteCaducidadUbicacion(
                    conn, lote, caducidad, ubicacionNombre);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int obtenerCantidadDisponibleDetalle(String idProducto, String lote, java.time.LocalDate caducidad,
                                                String presentacion, int factor, String ubicacionNombre) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaDetalle(
                    conn, idProducto, lote, caducidad, presentacion, factor, ubicacionNombre);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean existeLoteParaProducto(String lote, String idProducto) {
        try (Connection conn = new Conexion().conectar()) {
            int disponibles = GenericDAO.contarDisponiblesSinSalidaPorLoteProducto(conn, lote, idProducto);
            return disponibles > 0;
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
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY a.caducidad DESC LIMIT 1")) {

            int index = 1;
            ps.setString(index++, lote);
            ps.setString(index++, idProducto);
            index = agregarParametroEstado(ps, conn, index);
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
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarDisponiblesSinSalidaPorLoteProductoCaducidad(conn, lote, idProducto, caducidad);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public GenericDAO.ValidacionDisponibilidadSalida validarEntradaYDisponibilidadLoteProducto(String lote,
                                                                                                String idProducto) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.validarEntradaYDisponibilidadLoteProducto(conn, lote, idProducto);
        } catch (Exception e) {
            e.printStackTrace();
            return new GenericDAO.ValidacionDisponibilidadSalida(false, 0);
        }
    }

    public boolean existePresentacionParaProductoLote(String idProducto, String lote, String presentacion) {
        String sql = """
            SELECT 1
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, presentacion);
            index = agregarParametroEstado(ps, conn, index);
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
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn) + " LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerCantidadDisponibleProductoPresentacionFactor(String idProducto, String presentacion, int factor) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn))) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        return 0;
    }

    public List<DisponibilidadRapida> obtenerDisponibilidadesRapidas(String idProducto, String presentacion, int factor) {
        List<DisponibilidadRapida> resultado = new ArrayList<>();
        String sql = """
            SELECT a.lote, a.caducidad, u.nombre AS ubicacion, COUNT(*) AS total
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            JOIN ubicaciones u ON u.id = a.ubicacion
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
            GROUP BY a.lote, a.caducidad, u.nombre
            ORDER BY a.caducidad ASC, a.lote ASC
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn))) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            index = agregarParametroEstado(ps, conn, index);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String lote = rs.getString("lote");
                    java.sql.Date caducidad = rs.getDate("caducidad");
                    String ubicacion = rs.getString("ubicacion");
                    int total = rs.getInt("total");
                    java.time.LocalDate caducidadLocal = caducidad != null ? caducidad.toLocalDate() : null;
                    resultado.add(new DisponibilidadRapida(lote, caducidadLocal, ubicacion, total));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    public static class DisponibilidadRapida {
        private final String lote;
        private final java.time.LocalDate caducidad;
        private final String ubicacion;
        private final int total;

        public DisponibilidadRapida(String lote, java.time.LocalDate caducidad, String ubicacion, int total) {
            this.lote = lote != null ? lote : "";
            this.caducidad = caducidad;
            this.ubicacion = ubicacion != null ? ubicacion : "";
            this.total = total;
        }

        public String getLote() {
            return lote;
        }

        public java.time.LocalDate getCaducidad() {
            return caducidad;
        }

        public String getUbicacion() {
            return ubicacion;
        }

        public int getTotal() {
            return total;
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

    private String agregarFiltroEstado(String sqlBase, Connection conn) throws Exception {
        String colEstado = obtenerColumnaEstadoArticulo(conn);
        StringBuilder sql = new StringBuilder(sqlBase);
        if (colEstado != null) {
            String filtro = "LOWER(a." + colEstado + ") = ?";
            String sqlLower = sqlBase.toLowerCase();
            int insertPos = sql.length();
            int groupPos = sqlLower.indexOf(" group by ");
            int orderPos = sqlLower.indexOf(" order by ");
            if (groupPos >= 0 && orderPos >= 0) {
                insertPos = Math.min(groupPos, orderPos);
            } else if (groupPos >= 0) {
                insertPos = groupPos;
            } else if (orderPos >= 0) {
                insertPos = orderPos;
            }
            boolean tieneWhere = sqlLower.contains(" where ");
            String condicion = (tieneWhere ? " AND " : " WHERE ") + filtro + " ";
            if (insertPos < sql.length()) {
                sql.insert(insertPos, condicion);
            } else {
                sql.append(condicion);
            }
        }
        return sql.toString();
    }

    private int agregarParametroEstado(PreparedStatement ps, Connection conn, int index) throws Exception {
        String colEstado = obtenerColumnaEstadoArticulo(conn);
        if (colEstado != null) {
            ps.setString(index++, "disponible");
        }
        return index;
    }

    private String obtenerColumnaEstadoArticulo(Connection conn) throws Exception {
        java.sql.DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "articulo", null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String limpio = nombre.trim();
                String lower = limpio.toLowerCase();
                if ("estado".equals(lower)) {
                    return limpio;
                }
            }
        }
        return null;
    }
}
