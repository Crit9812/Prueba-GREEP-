package Operaciones.venta.model;

import Compartido.sesion.SesionUsuario;
import conexion.Conexion;
import Operaciones.traspasoSalida.model.traspasoSalida;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class model {

    public List<String> obtenerNombresClientes() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT Nombre FROM clientes WHERE status = 'activo' ORDER BY Nombre";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("Nombre"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public String obtenerIdClientePorNombre(String nombreCliente) {
        String sql = "SELECT id FROM clientes WHERE Nombre = ? AND status = 'activo' LIMIT 1";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreCliente);
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

    public boolean registrarVenta(String idDestinatario, String comentario, String numeroFactura, List<traspasoSalida> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        Connection conn = null;
        try {
            conn = new Conexion().conectar();
            conn.setAutoCommit(false);

            // Calcular totales
            BigDecimal totalNeto = BigDecimal.ZERO;
            BigDecimal totalGeneral = BigDecimal.ZERO;
            for (traspasoSalida item : items) {
                totalNeto = totalNeto.add(parseDecimal(item.getPrecioBruto()));
                totalGeneral = totalGeneral.add(parseDecimal(item.getPrecioTotal()));
            }

            // 1. Insertar en tabla salidas
            String sqlSalida = "INSERT INTO salidas (idDestinatario, noFactura, nota, fechaSalida, horaSalida, " +
                    "tipoSalida, Estado, claveUsuarioSalida, precioNetoSalida, precioTotalSalida) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            long idSalida;
            try (PreparedStatement ps = conn.prepareStatement(sqlSalida, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, idDestinatario);
                ps.setObject(2, parseEntero(numeroFactura));
                ps.setString(3, comentario != null ? comentario : "");
                ps.setDate(4, Date.valueOf(LocalDate.now()));
                ps.setTime(5, Time.valueOf(LocalTime.now()));
                ps.setString(6, "venta");
                ps.setString(7, "finalizado");

                Integer idUsuarioSalida = SesionUsuario.getIdUsuario();
                if (idUsuarioSalida != null) {
                    ps.setInt(8, idUsuarioSalida);
                } else {
                    ps.setNull(8, java.sql.Types.INTEGER);
                }

                ps.setBigDecimal(9, totalNeto);
                ps.setBigDecimal(10, totalGeneral);

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        idSalida = keys.getLong(1);
                    } else {
                        // Intentar con LAST_INSERT_ID
                        try (PreparedStatement psId = conn.prepareStatement("SELECT LAST_INSERT_ID()")) {
                            try (ResultSet rs = psId.executeQuery()) {
                                if (rs.next()) {
                                    idSalida = rs.getLong(1);
                                } else {
                                    conn.rollback();
                                    return false;
                                }
                            }
                        }
                    }
                }
            }

            // 2. Procesar cada item de la venta
            for (traspasoSalida item : items) {
                // Insertar en detalle_Salida
                String sqlDetalle = "INSERT INTO detalle_Salida (claveSalida, claveProductoSalida, cantidad, " +
                        "precioUnitarioSalida, precioIVASalida, precioBrutoTotalSalida, precioTotalSalida, " +
                        "Nota, estado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                long idDetalleSalida;
                try (PreparedStatement ps = conn.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, idSalida);
                    ps.setString(2, item.getClaveProducto());
                    ps.setInt(3, item.getCantidad());
                    ps.setBigDecimal(4, parseDecimal(item.getPrecioEntrada()));
                    ps.setBigDecimal(5, parseDecimal(item.getPrecioIva()));
                    ps.setBigDecimal(6, parseDecimal(item.getPrecioBruto()));
                    ps.setBigDecimal(7, parseDecimal(item.getPrecioTotal()));
                    ps.setString(8, item.getNota() != null ? item.getNota() : "");
                    ps.setString(9, "activo");

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            idDetalleSalida = keys.getLong(1);
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                // Verificar ubicaciones
                if (item.getUbicaciones().isEmpty()) {
                    conn.rollback();
                    return false;
                }

                // 3. Procesar cada ubicación
                for (Operaciones.compra.model.UbicacionCompra ubicacion : item.getUbicaciones()) {
                    if (ubicacion == null || ubicacion.getUbicacion() == null) {
                        continue;
                    }
                    int cantidad = Math.max(0, ubicacion.getCantidad());
                    if (cantidad == 0) {
                        continue;
                    }

                    Integer ubicacionId = resolverUbicacionId(conn, ubicacion.getUbicacion().trim());
                    if (ubicacionId == null) {
                        conn.rollback();
                        return false;
                    }

                    // Obtener detalle entrada ID
                    Integer detalleEntradaId = obtenerDetalleEntradaId(conn, item, ubicacionId);
                    if (detalleEntradaId == null) {
                        conn.rollback();
                        return false;
                    }

                    // Obtener artículos disponibles para vender
                    List<Integer> articulosParaEliminar = obtenerArticulosDisponibles(
                            conn, item, ubicacionId, cantidad, detalleEntradaId
                    );

                    if (articulosParaEliminar.size() < cantidad) {
                        conn.rollback();
                        return false;
                    }

                // Actualizar artículos (y sus detalleArticulo asociados) como vendidos
                    if (!actualizarArticulosVendidos(conn, articulosParaEliminar, idDetalleSalida)) {
                        conn.rollback();
                        return false;
                    }

                    // Actualizar estado de la entrada si es necesario
                    actualizarEstadoEntradaPorDetalle(conn, detalleEntradaId);
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            // Cerrar la conexión manualmente
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Integer obtenerDetalleEntradaId(Connection conn, traspasoSalida item, int ubicacionId) throws SQLException {
        String sql = "SELECT a.idDetalleEntrada AS detalleEntrada " +
                "FROM articulo a " +
                "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada " +
                "WHERE de.claveProducto = ? " +
                "AND a.lote = ? " +
                "AND a.ubicacion = ? " +
                "AND a.presentacion = ? " +
                "AND a.factor = ? ";

        Date caducidad = parseDate(item.getCaducidad());
        if (caducidad != null) {
            sql += "AND a.caducidad = ? ";
        } else {
            sql += "AND a.caducidad IS NULL ";
        }
        sql += "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            ps.setString(index++, item.getClaveProducto());
            ps.setString(index++, item.getLote());
            ps.setInt(index++, ubicacionId);
            ps.setString(index++, item.getPresentacion());
            ps.setInt(index++, item.getFactor());
            if (caducidad != null) {
                ps.setDate(index, caducidad);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("detalleEntrada");
                }
            }
        }

        return null;
    }

    private List<Integer> obtenerArticulosDisponibles(Connection conn, traspasoSalida item,
                                                      int ubicacionId, int cantidad, int detalleEntradaId) throws SQLException {
        List<Integer> articulosParaEliminar = new ArrayList<>();

        String sql = "SELECT a.idArticulo " +
                "FROM articulo a " +
                "WHERE a.idDetalleEntrada = ? " +
                "AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0) " +
                "AND a.lote = ? " +
                "AND a.ubicacion = ? " +
                "AND a.presentacion = ? " +
                "AND a.factor = ? " +
                "AND LOWER(a.Estado) = ? ";

        Date caducidad = parseDate(item.getCaducidad());
        if (caducidad != null) {
            sql += "AND a.caducidad = ? ";
        } else {
            sql += "AND a.caducidad IS NULL ";
        }
        sql += "LIMIT ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            ps.setInt(index++, detalleEntradaId);
            ps.setString(index++, item.getLote());
            ps.setInt(index++, ubicacionId);
            ps.setString(index++, item.getPresentacion());
            ps.setInt(index++, item.getFactor());
            ps.setString(index++, "disponible");
            if (caducidad != null) {
                ps.setDate(index++, caducidad);
            }
            ps.setInt(index, cantidad);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    articulosParaEliminar.add(rs.getInt("idArticulo"));
                }
            }
        }

        return articulosParaEliminar;
    }

    private boolean actualizarArticulosVendidos(Connection conn, List<Integer> articulosIds, long idDetalleSalida) throws SQLException {
        if (articulosIds.isEmpty()) {
            return false;
        }

        String placeholders = String.join(", ", java.util.Collections.nCopies(articulosIds.size(), "?"));
        String sqlUpdate = "UPDATE articulo SET idDetalleSalida = ?, Estado = ? WHERE idArticulo IN (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            int index = 1;
            ps.setLong(index++, idDetalleSalida);
            ps.setString(index++, "vendido");
            for (Integer idArticulo : articulosIds) {
                ps.setInt(index++, idArticulo);
            }

            int actualizadas = ps.executeUpdate();
            if (actualizadas < articulosIds.size()) {
                return false;
            }
        }

        // Reflejar salida sobre detalleArticulo ligado a los artículos vendidos
        String placeholdersDetalle = String.join(", ", java.util.Collections.nCopies(articulosIds.size(), "?"));
        String sqlUpdateDetalle = "UPDATE detalleArticulo " +
                "SET idDetalleSalida = ?, estado = ? " +
                "WHERE idArticulo IN (" + placeholdersDetalle + ") " +
                "AND (idDetalleSalida IS NULL OR idDetalleSalida = 0) " +
                "AND LOWER(estado) = ?";

        try (PreparedStatement psDetalle = conn.prepareStatement(sqlUpdateDetalle)) {
            int index = 1;
            psDetalle.setLong(index++, idDetalleSalida);
            psDetalle.setString(index++, "vendido");
            for (Integer idArticulo : articulosIds) {
                psDetalle.setInt(index++, idArticulo);
            }
            psDetalle.setString(index, "activo");
            psDetalle.executeUpdate();
        }

        return true;
    }

    private void actualizarEstadoEntradaPorDetalle(Connection conn, int detalleEntradaId) throws SQLException {
        // 1. Obtener claveEntrada como STRING
        String sqlClaveEntrada = "SELECT claveEntrada FROM detalle_Entrada WHERE idDetalleEntrada = ? LIMIT 1";
        String claveEntradaStr = null;

        try (PreparedStatement ps = conn.prepareStatement(sqlClaveEntrada)) {
            ps.setInt(1, detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    claveEntradaStr = rs.getString("claveEntrada");
                }
            }
        }

        if (claveEntradaStr == null) {
            return;
        }

        // 2. Determinar si es ajuste (tiene "A") o compra (numérico)
        boolean esAjuste = claveEntradaStr.toUpperCase().endsWith("A");

        // 3. Contar artículos por estado
        String sqlConteo = "SELECT LOWER(a.Estado) AS estado, COUNT(*) AS total " +
                "FROM articulo a " +
                "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada " +
                "WHERE a.idDetalleEntrada = ? " +
                "GROUP BY LOWER(a.Estado)";

        int disponibles = 0;
        int pendientes = 0;
        int vendidos = 0;
        int ajustados = 0;

        try (PreparedStatement ps = conn.prepareStatement(sqlConteo)) {
            ps.setInt(1, detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("estado");
                    int total = rs.getInt("total");
                    if ("disponible".equalsIgnoreCase(estado)) {
                        disponibles += total;
                    } else if ("pendiente".equalsIgnoreCase(estado)) {
                        pendientes += total;
                    } else if ("vendido".equalsIgnoreCase(estado)) {
                        vendidos += total;
                    } else if ("ajustado".equalsIgnoreCase(estado)) {
                        ajustados += total;
                    }
                }
            }
        }

        // 4. Determinar nuevo estado de la entrada principal
        String nuevoEstadoEntrada;
        if (disponibles > 0) {
            nuevoEstadoEntrada = "disponible";
        } else if (pendientes > 0) {
            nuevoEstadoEntrada = "pendiente";
        } else if (vendidos > 0 || ajustados > 0) {
            nuevoEstadoEntrada = "finalizado";
        } else {
            nuevoEstadoEntrada = "finalizado";
        }

        // 5. Actualizar tabla correspondiente (entradas o ajuste_inventario)
        if (esAjuste) {
            // Actualizar ajuste_inventario
            String sqlUpdate = "UPDATE ajuste_inventario SET estado = ? WHERE idAjuste = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setString(1, nuevoEstadoEntrada);
                ps.setString(2, claveEntradaStr);
                ps.executeUpdate();
            }
        } else {
            // Actualizar entradas (compra/traspaso)
            try {
                int claveEntradaNum = Integer.parseInt(claveEntradaStr);
                String sqlUpdate = "UPDATE entradas SET Estado = ? WHERE idEntrada = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setString(1, nuevoEstadoEntrada);
                    ps.setInt(2, claveEntradaNum);
                    ps.executeUpdate();
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: claveEntrada no es numérica ni ajuste: " + claveEntradaStr);
            }
        }

        // 6. Actualizar estado del detalle_Entrada específico
        String estadoDetalleEntrada = "activo"; // Estado por defecto
        if (disponibles == 0 && pendientes == 0) {
            // Si no hay artículos disponibles ni pendientes
            estadoDetalleEntrada = "desactivado";
        }

        // Actualizar detalle_Entrada
        String sqlUpdateDetalle = "UPDATE detalle_Entrada SET estado = ? WHERE idDetalleEntrada = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlUpdateDetalle)) {
            ps.setString(1, estadoDetalleEntrada);
            ps.setInt(2, detalleEntradaId);
            ps.executeUpdate();
        }
    }

    // Métodos auxiliares (se mantienen igual)
    private Integer parseEntero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Date parseDate(String fecha) {
        if (fecha == null || fecha.isBlank() ||
                fecha.equalsIgnoreCase("null") ||
                fecha.equalsIgnoreCase("n/a") ||
                fecha.trim().isEmpty()) {
            return null;
        }

        String fechaLimpia = fecha.trim();

        try {
            return Date.valueOf(fechaLimpia);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer resolverUbicacionId(Connection conn, String ubicacion) throws SQLException {
        if (ubicacion == null || ubicacion.isBlank()) {
            return null;
        }
        String texto = ubicacion.trim();
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException ignored) {
        }

        String sql = "SELECT id FROM ubicaciones WHERE nombre = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, texto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        return null;
    }
}
