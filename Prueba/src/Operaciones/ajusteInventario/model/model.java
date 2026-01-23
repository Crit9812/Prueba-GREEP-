package Operaciones.ajusteInventario.model;

import Compartido.sesion.SesionUsuario;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import Operaciones.traspasoSalida.model.traspasoSalida;
import conexion.Conexion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class model {

    public String registrarAjuste(List<compra> entradas, List<traspasoSalida> salidas, String comentario) {
        boolean sinEntradas = entradas == null || entradas.isEmpty();
        boolean sinSalidas = salidas == null || salidas.isEmpty();
        if (sinEntradas && sinSalidas) {
            return null;
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            Map<String, String> columnasAjuste = obtenerColumnas(conn, "ajuste_inventario");
            Map<String, Object> valoresAjuste = new LinkedHashMap<>();

            String colUsuario = resolverColumna(columnasAjuste, "idUsuario", "usuario", "usuario_id", "id_usuario");
            String colFecha = resolverColumna(columnasAjuste, "fechaAjuste", "fecha", "fecha_ajuste");
            String colHora = resolverColumna(columnasAjuste, "horaAjuste", "hora", "hora_ajuste");
            String colPrecioNeto = resolverColumna(columnasAjuste, "precioNeto", "precio_neto");
            String colPrecioTotal = resolverColumna(columnasAjuste, "precioTotal", "precio_total");
            String colNota = resolverColumna(columnasAjuste, "Nota", "nota", "comentario", "observaciones");

            Integer idUsuario = SesionUsuario.getIdUsuario();
            if (colUsuario != null && idUsuario != null) {
                valoresAjuste.put(colUsuario, idUsuario);
            }
            if (colFecha != null) {
                valoresAjuste.put(colFecha, LocalDate.now().toString());
            }
            if (colHora != null) {
                valoresAjuste.put(colHora, LocalTime.now().toSecondOfDay());
            }
            if (colNota != null) {
                valoresAjuste.put(colNota, comentario != null ? comentario : "");
            }

            BigDecimal totalNeto = BigDecimal.ZERO;
            BigDecimal totalGeneral = BigDecimal.ZERO;

            if (!sinEntradas) {
                for (compra item : entradas) {
                    BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
                    BigDecimal precioUnitario = parseDecimal(item.getPrecioEntrada());
                    BigDecimal precioTotal = parseDecimal(item.getPrecioTotal());
                    BigDecimal precioBruto = parseDecimal(item.getPrecioBruto());

                    totalNeto = totalNeto.add(precioUnitario.multiply(cantidad));
                    if (precioTotal.compareTo(BigDecimal.ZERO) > 0) {
                        totalGeneral = totalGeneral.add(precioTotal);
                    } else {
                        totalGeneral = totalGeneral.add(precioBruto.multiply(cantidad));
                    }
                }
            }

            if (!sinSalidas) {
                for (traspasoSalida item : salidas) {
                    BigDecimal precioBruto = parseDecimal(item.getPrecioBruto());
                    BigDecimal precioTotal = parseDecimal(item.getPrecioTotal());
                    totalNeto = totalNeto.subtract(precioBruto);
                    if (precioTotal.compareTo(BigDecimal.ZERO) > 0) {
                        totalGeneral = totalGeneral.subtract(precioTotal);
                    } else {
                        totalGeneral = totalGeneral.subtract(precioBruto);
                    }
                }
            }

            if (colPrecioNeto != null) {
                valoresAjuste.put(colPrecioNeto, totalNeto.setScale(2, RoundingMode.HALF_UP));
            }
            if (colPrecioTotal != null) {
                valoresAjuste.put(colPrecioTotal, totalGeneral.setScale(2, RoundingMode.HALF_UP));
            }

            long idAjuste = insertarRegistro(conn, "ajuste_inventario", columnasAjuste, valoresAjuste);

            if (!sinEntradas) {
                registrarDetallesEntrada(conn, idAjuste, entradas);
            }

            if (!sinSalidas) {
                registrarDetallesSalida(conn, idAjuste, salidas);
            }

            conn.commit();
            return String.valueOf(idAjuste);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void registrarDetallesEntrada(Connection conn, long idAjuste, List<compra> entradas) throws SQLException {
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

        String colEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colProducto = resolverColumna(columnasDetalle, "claveProducto", "idProducto", "id_producto", "producto_id");
        String colCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadEntrada");
        String colPrecioEntrada = resolverColumna(columnasDetalle, "precioUnitario", "precioEntrada", "precio_entrada", "costoEntrada");
        String colPrecioIva = resolverColumna(columnasDetalle, "precioIVA", "precioIva", "precio_iva");
        String colPrecioBruto = resolverColumna(columnasDetalle, "precioBrutoTotal", "precioBruto", "precio_bruto");
        String colPrecioTotalDetalle = resolverColumna(columnasDetalle, "precioTotal", "precio_total");
        String colNotaDetalle = resolverColumna(columnasDetalle, "Nota", "nota", "comentario", "observaciones");
        String colDetalleEstado = resolverColumna(columnasDetalle, "estado", "Estado");

        String colArticuloDetalleEntrada = resolverColumna(
                columnasArticulo,
                "idDetalleEntrada",
                "id_detalle_entrada",
                "detalleEntrada",
                "detalle_entrada",
                "detalle_entrada_id"
        );
        String colArticuloLote = resolverColumna(columnasArticulo, "lote");
        String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
        String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
        String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
        String colArticuloFactor = resolverColumna(columnasArticulo, "factor");
        String colArticuloSegmentado = resolverColumna(columnasArticulo, "segmentado");
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

        for (compra item : entradas) {
            Map<String, Object> valoresDetalle = new LinkedHashMap<>();

            if (colEntrada != null) valoresDetalle.put(colEntrada, idAjuste);
            if (colProducto != null) valoresDetalle.put(colProducto, item.getClaveProducto());
            if (colCantidad != null) valoresDetalle.put(colCantidad, item.getCantidad());
            if (colPrecioEntrada != null) valoresDetalle.put(colPrecioEntrada, parseDecimal(item.getPrecioEntrada()));
            if (colPrecioIva != null) valoresDetalle.put(colPrecioIva, parseDecimal(item.getPrecioIva()));
            if (colPrecioBruto != null) valoresDetalle.put(colPrecioBruto, parseDecimal(item.getPrecioBruto()));
            if (colPrecioTotalDetalle != null) valoresDetalle.put(colPrecioTotalDetalle, parseDecimal(item.getPrecioTotal()));
            if (colNotaDetalle != null) valoresDetalle.put(colNotaDetalle, item.getNota());
            if (colDetalleEstado != null) valoresDetalle.put(colDetalleEstado, "activo");

            long idDetalleEntrada = insertarRegistro(conn, "detalle_Entrada", columnasDetalle, valoresDetalle);

            Map<String, Integer> cantidadesPorUbicacion = new LinkedHashMap<>();
            for (UbicacionCompra ubicacion : item.getUbicaciones()) {
                if (ubicacion == null || ubicacion.getUbicacion() == null) {
                    continue;
                }
                int cantidadUbicacion = Math.max(0, ubicacion.getCantidad());
                if (cantidadUbicacion == 0) {
                    continue;
                }
                cantidadesPorUbicacion.merge(ubicacion.getUbicacion().trim(), cantidadUbicacion, Integer::sum);
            }

            for (Map.Entry<String, Integer> entry : cantidadesPorUbicacion.entrySet()) {
                Integer ubicacionId = resolverUbicacionId(conn, entry.getKey());
                int cantidadUbicacion = entry.getValue();
                for (int i = 0; i < cantidadUbicacion; i++) {
                    Map<String, Object> valoresArticulo = new LinkedHashMap<>();
                    if (colArticuloDetalleEntrada != null) valoresArticulo.put(colArticuloDetalleEntrada, idDetalleEntrada);
                    if (colArticuloLote != null) valoresArticulo.put(colArticuloLote, item.getLote());
                    if (colArticuloCaducidad != null) valoresArticulo.put(colArticuloCaducidad, parseDate(item.getCaducidad()));
                    if (colArticuloUbicacion != null) valoresArticulo.put(colArticuloUbicacion, ubicacionId);
                    if (colArticuloPresentacion != null) valoresArticulo.put(colArticuloPresentacion, item.getPresentacion());
                    if (colArticuloFactor != null) valoresArticulo.put(colArticuloFactor, parseInteger(item.getFactor()));
                    if (colArticuloSegmentado != null) valoresArticulo.put(colArticuloSegmentado, esSegmentado(item.getPresentacion()));
                    if (colArticuloEstado != null) valoresArticulo.put(colArticuloEstado, "disponible");

                    insertarRegistro(conn, "articulo", columnasArticulo, valoresArticulo);
                }
            }
        }
    }

    private void registrarDetallesSalida(Connection conn, long idAjuste, List<traspasoSalida> salidas) throws SQLException {
        Map<String, String> columnasDetalleSalida = obtenerColumnas(conn, "detalle_Salida");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");

        String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
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

        for (traspasoSalida item : salidas) {
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
            String colNotaDetalle = resolverColumna(columnasDetalleSalida, "Nota", "nota", "comentario", "observaciones");
            String colDetalleLote = resolverColumna(columnasDetalleSalida, "lote");
            String colDetalleCaducidad = resolverColumna(columnasDetalleSalida, "caducidad");
            String colDetallePresentacion = resolverColumna(columnasDetalleSalida, "presentacion");
            String colDetalleFactor = resolverColumna(columnasDetalleSalida, "factor");
            String colDetalleEstado = resolverColumna(columnasDetalleSalida, "estado", "Estado");

            if (colSalida != null) valoresDetalle.put(colSalida, idAjuste);
            if (colProducto != null) valoresDetalle.put(colProducto, item.getClaveProducto());
            if (colCantidad != null) valoresDetalle.put(colCantidad, item.getCantidad());
            if (colPrecioSalida != null) valoresDetalle.put(colPrecioSalida, parseDecimal(item.getPrecioEntrada()));
            if (colPrecioIva != null) valoresDetalle.put(colPrecioIva, parseDecimal(item.getPrecioIva()));
            if (colPrecioBruto != null) valoresDetalle.put(colPrecioBruto, parseDecimal(item.getPrecioBruto()));
            if (colPrecioTotalDetalle != null) valoresDetalle.put(colPrecioTotalDetalle, parseDecimal(item.getPrecioTotal()));
            if (colNotaDetalle != null) valoresDetalle.put(colNotaDetalle, item.getNota());
            if (colDetalleLote != null) valoresDetalle.put(colDetalleLote, item.getLote());
            if (colDetalleCaducidad != null) valoresDetalle.put(colDetalleCaducidad, parseDate(item.getCaducidad()));
            if (colDetallePresentacion != null) valoresDetalle.put(colDetallePresentacion, item.getPresentacion());
            if (colDetalleFactor != null) valoresDetalle.put(colDetalleFactor, item.getFactor());
            if (colDetalleEstado != null) valoresDetalle.put(colDetalleEstado, "activo");

            long idDetalleSalida = insertarRegistro(conn, "detalle_Salida", columnasDetalleSalida, valoresDetalle);

            if (item.getUbicaciones().isEmpty()) {
                throw new SQLException("No hay ubicaciones para la salida.");
            }

            for (UbicacionCompra ubicacion : item.getUbicaciones()) {
                if (ubicacion == null || ubicacion.getUbicacion() == null) {
                    continue;
                }
                int cantidad = Math.max(0, ubicacion.getCantidad());
                if (cantidad == 0) {
                    continue;
                }
                Integer ubicacionId = resolverUbicacionId(conn, ubicacion.getUbicacion().trim());
                if (ubicacionId == null) {
                    throw new SQLException("No se encontró la ubicación.");
                }

                Integer detalleEntradaId = obtenerDetalleEntradaId(conn, item, ubicacionId,
                        colArticuloDetalleEntrada, colDetalleEntradaId, colDetalleEntradaProducto,
                        colArticuloLote, colArticuloCaducidad, colArticuloPresentacion, colArticuloFactor,
                        colArticuloUbicacion);

                if (colArticuloId == null) {
                    throw new SQLException("No se encontró el id de articulo.");
                }

                StringBuilder sql = new StringBuilder("SELECT a.").append(colArticuloId)
                        .append(" FROM articulo a");
                if (colArticuloDetalleEntrada != null && colDetalleEntradaId != null) {
                    sql.append(" JOIN detalle_Entrada de ON de.")
                            .append(colDetalleEntradaId)
                            .append(" = a.")
                            .append(colArticuloDetalleEntrada);
                }
                sql.append(" WHERE 1=1");
                if (colArticuloDetalleSalida != null) {
                    sql.append(" AND (a.").append(colArticuloDetalleSalida).append(" IS NULL OR a.")
                            .append(colArticuloDetalleSalida).append(" = 0)");
                }
                if (colArticuloLote != null) {
                    sql.append(" AND a.").append(colArticuloLote).append(" = ?");
                }
                if (colArticuloCaducidad != null) {
                    sql.append(" AND a.").append(colArticuloCaducidad).append(" = ?");
                }
                if (colArticuloUbicacion != null) {
                    sql.append(" AND a.").append(colArticuloUbicacion).append(" = ?");
                }
                if (colArticuloPresentacion != null) {
                    sql.append(" AND a.").append(colArticuloPresentacion).append(" = ?");
                }
                if (colArticuloFactor != null) {
                    sql.append(" AND a.").append(colArticuloFactor).append(" = ?");
                }
                if (colArticuloEstado != null) {
                    sql.append(" AND LOWER(a.").append(colArticuloEstado).append(") = ?");
                }
                if (colDetalleEntradaProducto != null && colDetalleEntradaId != null && colArticuloDetalleEntrada != null) {
                    sql.append(" AND de.").append(colDetalleEntradaProducto).append(" = ?");
                }
                sql.append(" LIMIT ?");

                List<Integer> articulosParaActualizar = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
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
                    if (colDetalleEntradaProducto != null && colDetalleEntradaId != null
                            && colArticuloDetalleEntrada != null) {
                        ps.setString(index++, item.getClaveProducto());
                    }
                    ps.setInt(index, cantidad);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            articulosParaActualizar.add(rs.getInt(colArticuloId));
                        }
                    }
                }

                if (articulosParaActualizar.size() < cantidad) {
                    throw new SQLException("No hay suficientes artículos para la salida.");
                }

                if (colArticuloEstado != null || colArticuloDetalleSalida != null) {
                    String placeholders = String.join(", ", java.util.Collections.nCopies(articulosParaActualizar.size(), "?"));
                    StringBuilder sqlUpdate = new StringBuilder("UPDATE articulo SET ");
                    boolean agregaComa = false;
                    if (colArticuloDetalleSalida != null) {
                        sqlUpdate.append(colArticuloDetalleSalida).append(" = ?");
                        agregaComa = true;
                    }
                    if (colArticuloEstado != null) {
                        if (agregaComa) {
                            sqlUpdate.append(", ");
                        }
                        sqlUpdate.append(colArticuloEstado).append(" = ?");
                    }
                    sqlUpdate.append(" WHERE ").append(colArticuloId).append(" IN (").append(placeholders).append(")");
                    try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate.toString())) {
                        int index = 1;
                        if (colArticuloDetalleSalida != null) {
                            psUpdate.setLong(index++, idDetalleSalida);
                        }
                        if (colArticuloEstado != null) {
                            psUpdate.setString(index++, "ajustado");
                        }
                        for (Integer idArticulo : articulosParaActualizar) {
                            psUpdate.setInt(index++, idArticulo);
                        }
                        int actualizadas = psUpdate.executeUpdate();
                        if (actualizadas < cantidad) {
                            throw new SQLException("No se actualizaron todos los artículos.");
                        }
                    }
                } else {
                    String placeholders = String.join(", ", java.util.Collections.nCopies(articulosParaActualizar.size(), "?"));
                    String sqlDelete = "DELETE FROM articulo WHERE " + colArticuloId + " IN (" + placeholders + ")";
                    try (PreparedStatement psDelete = conn.prepareStatement(sqlDelete)) {
                        int index = 1;
                        for (Integer idArticulo : articulosParaActualizar) {
                            psDelete.setInt(index++, idArticulo);
                        }
                        int eliminadas = psDelete.executeUpdate();
                        if (eliminadas < cantidad) {
                            throw new SQLException("No se eliminaron todos los artículos.");
                        }
                    }
                }

                if (detalleEntradaId != null && colEntradaId != null && colEntradaEstado != null
                        && colDetalleEntradaClaveEntrada != null && colDetalleEntradaId != null
                        && colArticuloDetalleEntrada != null && colArticuloEstado != null) {
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
            }
        }
    }

    private Integer obtenerDetalleEntradaId(Connection conn, traspasoSalida item, int ubicacionId,
                                            String colArticuloDetalleEntrada, String colDetalleEntradaId,
                                            String colDetalleEntradaProducto, String colArticuloLote,
                                            String colArticuloCaducidad, String colArticuloPresentacion,
                                            String colArticuloFactor, String colArticuloUbicacion) throws SQLException {
        if (colArticuloDetalleEntrada == null || colDetalleEntradaId == null || colDetalleEntradaProducto == null) {
            return null;
        }
        StringBuilder sql = new StringBuilder("SELECT a.").append(colArticuloDetalleEntrada)
                .append(" AS detalleEntrada FROM articulo a JOIN detalle_Entrada de ON de.")
                .append(colDetalleEntradaId).append(" = a.").append(colArticuloDetalleEntrada)
                .append(" WHERE de.").append(colDetalleEntradaProducto).append(" = ?");

        if (colArticuloLote != null) {
            sql.append(" AND a.").append(colArticuloLote).append(" = ?");
        }
        if (colArticuloCaducidad != null) {
            sql.append(" AND a.").append(colArticuloCaducidad).append(" = ?");
        }
        if (colArticuloPresentacion != null) {
            sql.append(" AND a.").append(colArticuloPresentacion).append(" = ?");
        }
        if (colArticuloFactor != null) {
            sql.append(" AND a.").append(colArticuloFactor).append(" = ?");
        }
        if (colArticuloUbicacion != null) {
            sql.append(" AND a.").append(colArticuloUbicacion).append(" = ?");
        }
        sql.append(" LIMIT 1");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setString(index++, item.getClaveProducto());
            if (colArticuloLote != null) {
                ps.setString(index++, item.getLote());
            }
            if (colArticuloCaducidad != null) {
                ps.setDate(index++, parseDate(item.getCaducidad()));
            }
            if (colArticuloPresentacion != null) {
                ps.setString(index++, item.getPresentacion());
            }
            if (colArticuloFactor != null) {
                ps.setInt(index++, item.getFactor());
            }
            if (colArticuloUbicacion != null) {
                ps.setInt(index, ubicacionId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("detalleEntrada");
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
        int vendidos = 0;
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
                    } else if ("vendido".equalsIgnoreCase(estado)) {
                        vendidos += total;
                    }
                }
            }
        }

        String nuevoEstado;
        if (disponibles > 0) {
            nuevoEstado = "disponible";
        } else if (pendientes > 0) {
            nuevoEstado = "pendiente";
        } else if (vendidos > 0) {
            nuevoEstado = "finalizado";
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
            if (candidato == null) {
                continue;
            }
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
        String limpio = valor.trim().replace(",", "");
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer parseInteger(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
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

        String insertar = "INSERT INTO ubicaciones (nombre) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, texto);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return null;
    }

    private int esSegmentado(String presentacion) {
        return 0;
    }
}
