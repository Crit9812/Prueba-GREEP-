package Operaciones.traspasoSalida.model;

import Compartido.sesion.SesionUsuario;
import conexion.Conexion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class model {

    public List<String> obtenerNombresSucursales() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM sucursales WHERE status = 'activo' ORDER BY nombre";

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

    public String obtenerIdSucursalPorNombre(String nombreSucursal) {
        String sql = "SELECT id FROM sucursales WHERE nombre = ? AND status = 'activo' LIMIT 1";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreSucursal);
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

    public String registrarTraspasoSalida(String idDestinatario, String comentario, List<traspasoSalida> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            Map<String, String> columnasSalida = obtenerColumnas(conn, "salidas");
            Map<String, Object> valoresSalida = new LinkedHashMap<>();

            String colDestinatario = resolverColumna(columnasSalida, "idDestinatario", "destinatario", "idSucursal",
                    "id_sucursal", "sucursal", "sucursal_id");
            String colFactura = resolverColumna(columnasSalida, "noFatura", "noFactura", "factura",
                    "numeroFactura", "numero_factura");
            String colComentario = resolverColumna(columnasSalida, "nota", "comentario", "observaciones");
            String colFecha = resolverColumna(columnasSalida, "fechaSalida", "fecha", "fecha_salida", "created_at");
            String colHora = resolverColumna(columnasSalida, "horaSalida", "hora", "hora_salida");
            String colTipo = resolverColumna(columnasSalida, "tipoSalida", "tipo", "tipo_salida");
            String colPrecioNeto = resolverColumna(columnasSalida, "precioNetoSalida", "precioNeto",
                    "precio_neto", "precioNetoTotal");
            String colPrecioTotal = resolverColumna(columnasSalida, "precioTotalSalida", "precioTotal", "precio_total");
            String colUsuarioSalida = resolverColumna(columnasSalida, "claveUsuarioSalida", "idUsuarioSalida",
                    "id_usuario_salida", "usuarioSalida", "usuario_salida");
            String colEstado = resolverColumna(columnasSalida, "Estado", "estado");

            if (colDestinatario != null) valoresSalida.put(colDestinatario, idDestinatario);
            if (colFactura != null) valoresSalida.put(colFactura, null);
            if (colComentario != null) valoresSalida.put(colComentario, comentario != null ? comentario : "");
            if (colFecha != null) valoresSalida.put(colFecha, Date.valueOf(LocalDate.now()));
            if (colHora != null) valoresSalida.put(colHora, Time.valueOf(LocalTime.now()));
            if (colTipo != null) valoresSalida.put(colTipo, "traspaso");
            if (colEstado != null) valoresSalida.put(colEstado, "pendiente");
            Integer idUsuarioSalida = SesionUsuario.getIdUsuario();
            if (colUsuarioSalida != null && idUsuarioSalida != null) {
                valoresSalida.put(colUsuarioSalida, idUsuarioSalida);
            }

            BigDecimal totalNeto = BigDecimal.ZERO;
            BigDecimal totalGeneral = BigDecimal.ZERO;
            for (traspasoSalida item : items) {
                BigDecimal precioBruto = parseDecimal(item.getPrecioBruto());
                BigDecimal precioTotal = parseDecimal(item.getPrecioTotal());
                totalNeto = totalNeto.add(precioBruto);
                totalGeneral = totalGeneral.add(precioTotal);
            }

            if (colPrecioNeto != null) {
                valoresSalida.put(colPrecioNeto, totalNeto.setScale(2, RoundingMode.HALF_UP));
            }
            if (colPrecioTotal != null) {
                valoresSalida.put(colPrecioTotal, totalGeneral.setScale(2, RoundingMode.HALF_UP));
            }

            long idSalida = insertarRegistro(conn, "salidas", columnasSalida, valoresSalida);

            // Validar que se obtuvo un ID válido
            if (idSalida <= 0) {
                conn.rollback();
                return null;
            }

            Map<String, String> columnasDetalleSalida = obtenerColumnas(conn, "detalle_Salida");
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
            Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");

            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            if (colArticuloDetalleSalida == null) {
                conn.rollback();
                return null;
            }

            String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada",
                    "id_detalle_entrada", "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
            String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                    "id_detalle_entrada");
            String colDetalleEntradaProducto = resolverColumna(columnasDetalleEntrada, "claveProducto", "idProducto",
                    "id_producto", "producto_id");
            String colDetalleEntradaClaveEntrada = resolverColumna(columnasDetalleEntrada, "claveEntrada", "idEntrada",
                    "entrada_id", "id_entrada");

            String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
            String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
            String colArticuloLote = resolverColumna(columnasArticulo, "lote");
            String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
            String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
            String colArticuloFactor = resolverColumna(columnasArticulo, "factor");
            String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
            String colEntradaEstado = resolverColumna(columnasEntradas, "Estado", "estado");
            String colEntradaId = resolverColumna(columnasEntradas, "idEntrada", "id", "id_entrada");

            if (colArticuloId == null || colArticuloDetalleEntrada == null || colDetalleEntradaId == null
                    || colDetalleEntradaClaveEntrada == null || colEntradaEstado == null || colEntradaId == null) {
                conn.rollback();
                return null;
            }

            java.util.Set<Integer> detallesEntradaActualizados = new java.util.HashSet<>();

            for (traspasoSalida item : items) {
                Map<String, Object> valoresDetalle = new LinkedHashMap<>();

                String colSalida = resolverColumna(columnasDetalleSalida, "claveSalida", "idSalida", "id_salida",
                        "salida_id");
                String colProducto = resolverColumna(columnasDetalleSalida, "claveProductoSalida", "claveProducto",
                        "idProducto", "id_producto", "producto_id");
                String colCantidad = resolverColumna(columnasDetalleSalida, "cantidad", "cantidadSalida",
                        "cantidad_salida");
                String colPrecioSalida = resolverColumna(columnasDetalleSalida, "precioUnitarioSalida", "precioUnitario",
                        "precioSalida", "precio_salida", "precioSalidaUnitario");
                String colPrecioIva = resolverColumna(columnasDetalleSalida, "precioIVASalida", "precioIVA", "precioIva",
                        "precio_iva");
                String colPrecioBruto = resolverColumna(columnasDetalleSalida, "precioBrutoTotalSalida",
                        "precioBrutoTotal", "precioBruto", "precio_bruto");
                String colPrecioTotalDetalle = resolverColumna(columnasDetalleSalida, "precioTotalSalida", "precioTotal",
                        "precio_total");
                String colDetalleLote = resolverColumna(columnasDetalleSalida, "lote");
                String colDetalleCaducidad = resolverColumna(columnasDetalleSalida, "caducidad");
                String colDetallePresentacion = resolverColumna(columnasDetalleSalida, "presentacion");
                String colDetalleFactor = resolverColumna(columnasDetalleSalida, "factor");

                if (colSalida != null) valoresDetalle.put(colSalida, idSalida);
                if (colProducto != null) valoresDetalle.put(colProducto, item.getClaveProducto());
                if (colCantidad != null) valoresDetalle.put(colCantidad, item.getCantidad());
                if (colPrecioSalida != null) valoresDetalle.put(colPrecioSalida, parseDecimal(item.getPrecioEntrada()));
                if (colPrecioIva != null) valoresDetalle.put(colPrecioIva, parseDecimal(item.getPrecioIva()));
                if (colPrecioBruto != null) valoresDetalle.put(colPrecioBruto, parseDecimal(item.getPrecioBruto()));
                if (colPrecioTotalDetalle != null) {
                    valoresDetalle.put(colPrecioTotalDetalle, parseDecimal(item.getPrecioTotal()));
                }
                if (colDetalleLote != null) valoresDetalle.put(colDetalleLote, item.getLote());
                if (colDetalleCaducidad != null) valoresDetalle.put(colDetalleCaducidad, parseDate(item.getCaducidad()));
                if (colDetallePresentacion != null) valoresDetalle.put(colDetallePresentacion, item.getPresentacion());
                if (colDetalleFactor != null) valoresDetalle.put(colDetalleFactor, item.getFactor());

                long idDetalleSalida = insertarRegistro(conn, "detalle_Salida", columnasDetalleSalida, valoresDetalle);

                if (item.getUbicaciones().isEmpty()) {
                    conn.rollback();
                    return null;
                }

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
                        return null;
                    }

                    List<Integer> articulosParaActualizar = new ArrayList<>();
                    List<Integer> detallesEntrada = new ArrayList<>();

                    StringBuilder sqlSelect = new StringBuilder("SELECT a.")
                            .append(colArticuloId)
                            .append(", a.")
                            .append(colArticuloDetalleEntrada)
                            .append(" FROM articulo a JOIN detalle_Entrada de ON de.")
                            .append(colDetalleEntradaId)
                            .append(" = a.")
                            .append(colArticuloDetalleEntrada)
                            .append(" WHERE 1=1");
                    if (colArticuloDetalleSalida != null) {
                        sqlSelect.append(" AND (a.").append(colArticuloDetalleSalida)
                                .append(" IS NULL OR a.")
                                .append(colArticuloDetalleSalida)
                                .append(" = 0)");
                    }
                    if (colArticuloLote != null) {
                        sqlSelect.append(" AND a.").append(colArticuloLote).append(" = ?");
                    }
                    if (colArticuloCaducidad != null) {
                        sqlSelect.append(" AND a.").append(colArticuloCaducidad).append(" = ?");
                    }
                    if (colArticuloUbicacion != null) {
                        sqlSelect.append(" AND a.").append(colArticuloUbicacion).append(" = ?");
                    }
                    if (colArticuloPresentacion != null) {
                        sqlSelect.append(" AND a.").append(colArticuloPresentacion).append(" = ?");
                    }
                    if (colArticuloFactor != null) {
                        sqlSelect.append(" AND a.").append(colArticuloFactor).append(" = ?");
                    }
                    if (colArticuloEstado != null) {
                        sqlSelect.append(" AND LOWER(a.").append(colArticuloEstado).append(") = ?");
                    }
                    if (colDetalleEntradaProducto != null) {
                        sqlSelect.append(" AND de.").append(colDetalleEntradaProducto).append(" = ?");
                    }
                    sqlSelect.append(" LIMIT ?");

                    try (PreparedStatement ps = conn.prepareStatement(sqlSelect.toString())) {
                        int index = 1;
                        if (colArticuloLote != null) {
                            ps.setString(index++, item.getLote());
                        }
                        if (colArticuloCaducidad != null) {
                            ps.setDate(index++, parseDate(item.getCaducidad()));
                        }
                        if (colArticuloUbicacion != null) {
                            ps.setInt(index++, ubicacionId);
                        }
                        if (colArticuloPresentacion != null) {
                            ps.setString(index++, item.getPresentacion());
                        }
                        if (colArticuloFactor != null) {
                            ps.setInt(index++, item.getFactor());
                        }
                        if (colArticuloEstado != null) {
                            ps.setString(index++, "disponible");
                        }
                        if (colDetalleEntradaProducto != null) {
                            ps.setString(index++, item.getClaveProducto());
                        }
                        ps.setInt(index, cantidad);

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                articulosParaActualizar.add(rs.getInt(colArticuloId));
                                detallesEntrada.add(rs.getInt(colArticuloDetalleEntrada));
                            }
                        }
                    }

                    if (articulosParaActualizar.size() < cantidad) {
                        conn.rollback();
                        return null;
                    }

                    String placeholders = String.join(", ", java.util.Collections.nCopies(articulosParaActualizar.size(), "?"));
                    StringBuilder sqlUpdate = new StringBuilder("UPDATE articulo SET ")
                            .append(colArticuloDetalleSalida)
                            .append(" = ?");
                    if (colArticuloEstado != null) {
                        sqlUpdate.append(", ").append(colArticuloEstado).append(" = ?");
                    }
                    sqlUpdate.append(" WHERE ").append(colArticuloId).append(" IN (").append(placeholders).append(")");

                    try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate.toString())) {
                        int index = 1;
                        psUpdate.setLong(index++, idDetalleSalida);
                        if (colArticuloEstado != null) {
                            psUpdate.setString(index++, "pendiente");
                        }
                        for (Integer idArticulo : articulosParaActualizar) {
                            psUpdate.setInt(index++, idArticulo);
                        }
                        int actualizadas = psUpdate.executeUpdate();
                        if (actualizadas < cantidad) {
                            conn.rollback();
                            return null;
                        }
                    }

                    detallesEntradaActualizados.addAll(detallesEntrada);
                }
            }

            for (Integer detalleEntradaId : detallesEntradaActualizados) {
                actualizarEstadoEntradaPorDetalle(
                        conn,
                        detalleEntradaId,
                        colEntradaId,
                        colEntradaEstado,
                        colDetalleEntradaId,
                        colDetalleEntradaClaveEntrada,
                        colArticuloDetalleEntrada,
                        colArticuloEstado
                );
            }

            conn.commit();
            // Retornar el idSalida real como string
            return String.valueOf(idSalida);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new java.util.HashMap<>();
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

        if (columnas.isEmpty()) {
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla.toLowerCase(), null)) {
                while (rs.next()) {
                    String nombre = rs.getString("COLUMN_NAME");
                    if (nombre == null) {
                        continue;
                    }
                    String nombreLimpio = nombre.trim();
                    columnas.put(nombreLimpio.toLowerCase(), nombreLimpio);
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
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return Date.valueOf(fecha);
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

    private void actualizarEstadoEntradaPorDetalle(Connection conn,
                                                   int detalleEntradaId,
                                                   String colEntradaId,
                                                   String colEntradaEstado,
                                                   String colDetalleEntradaId,
                                                   String colDetalleEntradaClaveEntrada,
                                                   String colArticuloDetalleEntrada,
                                                   String colArticuloEstado) throws SQLException {
        if (colEntradaId == null || colEntradaEstado == null || colDetalleEntradaId == null
                || colDetalleEntradaClaveEntrada == null || colArticuloDetalleEntrada == null
                || colArticuloEstado == null) {
            return;
        }

        Integer claveEntrada = obtenerClaveEntrada(conn, detalleEntradaId, colDetalleEntradaId, colDetalleEntradaClaveEntrada);
        if (claveEntrada == null) {
            return;
        }

        String sqlConteo = "SELECT LOWER(a." + colArticuloEstado + ") AS estado, COUNT(*) AS total " +
                "FROM articulo a JOIN detalle_Entrada de ON de." + colDetalleEntradaId + " = a." +
                colArticuloDetalleEntrada + " WHERE de." + colDetalleEntradaClaveEntrada + " = ? " +
                "GROUP BY LOWER(a." + colArticuloEstado + ")";

        int disponibles = 0;
        int pendientes = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlConteo)) {
            ps.setInt(1, claveEntrada);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("estado");
                    int total = rs.getInt("total");
                    if ("disponible".equalsIgnoreCase(estado)) {
                        disponibles += total;
                    } else if ("pendiente".equalsIgnoreCase(estado)) {
                        pendientes += total;
                    }
                }
            }
        }

        String nuevoEstado;
        if (disponibles > 0) {
            nuevoEstado = "disponible";
        } else if (pendientes > 0) {
            nuevoEstado = "pendiente";
        } else {
            nuevoEstado = "finalizado";
        }

        String sqlUpdate = "UPDATE entradas SET " + colEntradaEstado + " = ? WHERE " + colEntradaId + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, claveEntrada);
            ps.executeUpdate();
        }
    }

    private Integer obtenerClaveEntrada(Connection conn, int detalleEntradaId, String colDetalleEntradaId,
                                        String colDetalleEntradaClaveEntrada) throws SQLException {
        String sql = "SELECT " + colDetalleEntradaClaveEntrada + " AS claveEntrada FROM detalle_Entrada WHERE "
                + colDetalleEntradaId + " = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("claveEntrada");
                }
            }
        }
        return null;
    }
}
