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
        String sql = "SELECT nombre FROM ubicaciones WHERE LOWER(estado) = 'activo' ORDER BY nombre";

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
              AND (a.caducidad = ? OR (a.caducidad IS NULL AND ? IS NULL))
              AND u.nombre = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY de.idDetalleEntrada DESC LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote != null ? lote : "");
            if (caducidad != null) {
                ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                ps.setDate(index++, java.sql.Date.valueOf(caducidad));
            } else {
                ps.setDate(index++, null);
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
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND (a.caducidad = ? OR (a.caducidad IS NULL AND ? IS NULL))
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY de.idDetalleEntrada DESC LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote != null ? lote : "");
            if (caducidad != null) {
                ps.setDate(index++, java.sql.Date.valueOf(caducidad));
                ps.setDate(index++, java.sql.Date.valueOf(caducidad));
            } else {
                ps.setDate(index++, null);
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

    public Optional<PreciosProducto> obtenerPreciosProductoPorLotePresentacion(String idProducto, String lote,
                                                                               String presentacion) {
        // Buscar precios sin depender de la caducidad
        String sql = """
        SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal
        FROM detalle_Entrada de
        JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
        WHERE de.claveProducto = ?
          AND a.lote = ?
          AND a.presentacion = ?
          AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
          AND LOWER(a.estado) = 'disponible'
        ORDER BY 
            CASE 
                WHEN a.caducidad IS NOT NULL THEN 0
                ELSE 1
            END,
            de.idDetalleEntrada DESC
        LIMIT 1
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, presentacion);

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

    public Optional<PreciosProducto> obtenerPreciosProductoPorLotePresentacionFactor(String idProducto, String lote,
                                                                                     String presentacion, int factor) {
        String sql = """
            SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal
            FROM detalle_Entrada de
            JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND a.presentacion = ?
              AND a.factor = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY de.idDetalleEntrada DESC LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote != null ? lote : "");
            ps.setString(index++, presentacion != null ? presentacion : "");
            ps.setInt(index++, factor);
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
            WHERE de.claveProducto = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY de.idDetalleEntrada DESC LIMIT 1")) {

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
        int disponibles = contarDisponiblesConDetalle(null, lote, caducidad, true,
                null, null, ubicacionNombre);
        return disponibles > 0;
    }

    public int obtenerCantidadDisponible(String lote, java.time.LocalDate caducidad, String ubicacionNombre) {
        return contarDisponiblesConDetalle(null, lote, caducidad, true,
                null, null, ubicacionNombre);
    }

    public boolean existeLoteCaducidadUbicacionProducto(String idProducto, String lote,
                                                        java.time.LocalDate caducidad, String ubicacionNombre) {
        int disponibles = contarDisponiblesConDetalle(idProducto, lote, caducidad, true,
                null, null, ubicacionNombre);
        return disponibles > 0;
    }

    public int obtenerCantidadDisponibleProductoUbicacion(String idProducto, String lote,
                                                          java.time.LocalDate caducidad, String ubicacionNombre) {
        return contarDisponiblesConDetalle(idProducto, lote, caducidad, true,
                null, null, ubicacionNombre);
    }

    public int obtenerCantidadDisponibleDetalle(String idProducto, String lote, java.time.LocalDate caducidad,
                                                String presentacion, int factor, String ubicacionNombre) {
        return contarDisponiblesConDetalle(idProducto, lote, caducidad, true,
                presentacion, factor, ubicacionNombre);
    }

    public boolean existeLoteParaProducto(String lote, String idProducto) {
        String sql = """
        SELECT COUNT(*) AS total FROM (
            SELECT da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) t
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, lote);
            ps.setString(3, idProducto);
            ps.setString(4, lote);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public Optional<java.time.LocalDate> obtenerCaducidadParaLoteProducto(String lote, String idProducto) {
        String sql = """
        SELECT x.caducidad
        FROM (
            SELECT a.caducidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE a.lote = ? AND de.claveProducto = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT a.caducidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE a.lote = ? AND de.claveProducto = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) x
        ORDER BY 
            CASE 
                WHEN x.caducidad IS NOT NULL THEN 0
                ELSE 1
            END,
            x.caducidad ASC
        LIMIT 1
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lote);
            ps.setString(2, idProducto);
            ps.setString(3, lote);
            ps.setString(4, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date caducidad = rs.getDate("caducidad");
                    if (caducidad != null && !rs.wasNull()) {
                        return Optional.of(caducidad.toLocalDate());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public boolean existeProductoLoteSinCaducidad(String idProducto, String lote) {
        String sql = """
        SELECT 1
        FROM (
            SELECT da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND a.caducidad IS NULL
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND a.caducidad IS NULL
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) x
        LIMIT 1
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, lote);
            ps.setString(3, idProducto);
            ps.setString(4, lote);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerCantidadDisponibleProductoLoteCaducidad(String idProducto, String lote,
                                                              java.time.LocalDate caducidad) {
        return contarDisponiblesConDetalle(idProducto, lote, caducidad, true,
                null, null, null);
    }

    public GenericDAO.ValidacionDisponibilidadSalida validarEntradaYDisponibilidadLoteProducto(String lote,
                                                                                               String idProducto) {
        int total = contarUnidadesTotalesLoteProducto(idProducto, lote);
        int disponibles = contarDisponiblesConDetalle(idProducto, lote, null, false,
                null, null, null);
        return new GenericDAO.ValidacionDisponibilidadSalida(total > 0, disponibles);
    }

    public boolean existePresentacionParaProductoLote(String idProducto, String lote, String presentacion) {
        String sql = """
            SELECT 1
            FROM (
                SELECT da.idDetalle AS unidad
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                UNION ALL
                SELECT CAST(a.idArticulo AS CHAR) AS unidad
                FROM articulo a
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ?
                  AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
                  AND LOWER(a.estado) = 'disponible'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM detalleArticulo da
                        WHERE da.idArticulo = a.idArticulo
                          AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                          AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                  )
            ) x
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, presentacion);
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index, presentacion);
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
            FROM (
                SELECT da.idDetalle AS unidad
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ? AND a.factor = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                UNION ALL
                SELECT CAST(a.idArticulo AS CHAR) AS unidad
                FROM articulo a
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ? AND a.lote = ? AND a.presentacion = ? AND a.factor = ?
                  AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
                  AND LOWER(a.estado) = 'disponible'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM detalleArticulo da
                        WHERE da.idArticulo = a.idArticulo
                          AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                          AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                  )
            ) x
            LIMIT 1
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            ps.setString(index++, idProducto);
            ps.setString(index++, lote);
            ps.setString(index++, presentacion);
            ps.setInt(index, factor);
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
            SELECT COUNT(*) AS total FROM (
                SELECT da.idDetalle AS unidad
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                UNION ALL
                SELECT CAST(a.idArticulo AS CHAR) AS unidad
                FROM articulo a
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
                  AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
                  AND LOWER(a.estado) = 'disponible'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM detalleArticulo da
                        WHERE da.idArticulo = a.idArticulo
                          AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                          AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                  )
            ) t
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index, factor);
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
            SELECT z.lote, z.caducidad, z.ubicacion, COUNT(*) AS total
            FROM (
                SELECT a.lote, a.caducidad, u.nombre AS ubicacion, da.idDetalle AS unidad
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                JOIN ubicaciones u ON u.id = da.idUbicacion
                WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                UNION ALL
                SELECT a.lote, a.caducidad, u.nombre AS ubicacion, CAST(a.idArticulo AS CHAR) AS unidad
                FROM articulo a
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                JOIN ubicaciones u ON u.id = a.ubicacion
                WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
                  AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
                  AND LOWER(a.estado) = 'disponible'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM detalleArticulo da
                        WHERE da.idArticulo = a.idArticulo
                          AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                          AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                  )
            ) z
            GROUP BY z.lote, z.caducidad, z.ubicacion
            ORDER BY 
                CASE 
                    WHEN z.caducidad IS NOT NULL THEN 0
                    ELSE 1
                END,
                z.caducidad ASC, 
                z.lote ASC
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index, factor);
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

    /**
     * Obtiene la cantidad disponible para un producto con lote, caducidad, presentación y factor específicos
     */
    public int obtenerCantidadDisponibleProductoLoteCaducidadPresentacionFactor(
            String idProducto, String lote, java.time.LocalDate caducidad,
            String presentacion, int factor) {
        return contarDisponiblesConDetalle(idProducto, lote, caducidad, true,
                presentacion, factor, null);
    }

    public int obtenerCantidadDisponibleProductoLote(String idProducto, String lote) {
        return contarDisponiblesConDetalle(idProducto, lote, null, false,
                null, null, null);
    }

    private int contarDisponiblesConDetalle(String idProducto, String lote, java.time.LocalDate caducidad,
                                            boolean filtrarCaducidad, String presentacion, Integer factor,
                                            String ubicacionNombre) {
        StringBuilder detalleWhere = new StringBuilder();
        StringBuilder articuloWhere = new StringBuilder();
        List<Object> parametros = new ArrayList<>();

        agregarFiltrosBase(detalleWhere, parametros, idProducto, lote, caducidad,
                filtrarCaducidad, presentacion, factor, ubicacionNombre, true);
        List<Object> parametrosArticulo = new ArrayList<>();
        agregarFiltrosBase(articuloWhere, parametrosArticulo, idProducto, lote, caducidad,
                filtrarCaducidad, presentacion, factor, ubicacionNombre, false);

        String sql = """
            SELECT COUNT(*) AS total
            FROM (
                SELECT da.idDetalle AS unidad
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                LEFT JOIN ubicaciones u ON u.id = da.idUbicacion
                WHERE 1 = 1
        """ + detalleWhere + """
                  AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                  AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                UNION ALL
                SELECT CAST(a.idArticulo AS CHAR) AS unidad
                FROM articulo a
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                LEFT JOIN ubicaciones u ON u.id = a.ubicacion
                WHERE 1 = 1
        """ + articuloWhere + """
                  AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
                  AND LOWER(a.estado) = 'disponible'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM detalleArticulo da
                        WHERE da.idArticulo = a.idArticulo
                          AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                          AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
                  )
            ) t
        """;

        parametros.addAll(parametrosArticulo);
        return ejecutarConteo(sql, parametros);
    }

    private int contarUnidadesTotalesLoteProducto(String idProducto, String lote) {
        List<Object> parametros = new ArrayList<>();
        StringBuilder where = new StringBuilder();

        if (idProducto != null && !idProducto.isBlank()) {
            where.append(" AND de.claveProducto = ?");
            parametros.add(idProducto);
        }
        if (lote != null && !lote.isBlank()) {
            where.append(" AND a.lote = ?");
            parametros.add(lote);
        }

        String sql = """
            SELECT COUNT(*) AS total
            FROM (
                SELECT da.idDetalle AS unidad
                FROM detalleArticulo da
                JOIN articulo a ON a.idArticulo = da.idArticulo
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE 1 = 1
        """ + where + """
                UNION ALL
                SELECT CAST(a.idArticulo AS CHAR) AS unidad
                FROM articulo a
                JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
                WHERE 1 = 1
        """ + where + """
                  AND NOT EXISTS (
                        SELECT 1 FROM detalleArticulo da
                        WHERE da.idArticulo = a.idArticulo
                  )
            ) t
        """;

        List<Object> parametrosArticulo = new ArrayList<>(parametros);
        parametros.addAll(parametrosArticulo);
        return ejecutarConteo(sql, parametros);
    }

    private void agregarFiltrosBase(StringBuilder where, List<Object> parametros, String idProducto, String lote,
                                    java.time.LocalDate caducidad, boolean filtrarCaducidad,
                                    String presentacion, Integer factor, String ubicacionNombre,
                                    boolean esDetalle) {
        if (idProducto != null && !idProducto.isBlank()) {
            where.append(" AND de.claveProducto = ?");
            parametros.add(idProducto);
        }
        if (lote != null && !lote.isBlank()) {
            where.append(" AND a.lote = ?");
            parametros.add(lote);
        }
        if (filtrarCaducidad) {
            where.append(" AND (a.caducidad = ? OR (a.caducidad IS NULL AND ? IS NULL))");
            java.sql.Date fecha = caducidad != null ? java.sql.Date.valueOf(caducidad) : null;
            parametros.add(fecha);
            parametros.add(fecha);
        }
        if (presentacion != null && !presentacion.isBlank()) {
            where.append(" AND a.presentacion = ?");
            parametros.add(presentacion);
        }
        if (factor != null) {
            where.append(" AND a.factor = ?");
            parametros.add(factor);
        }
        if (ubicacionNombre != null && !ubicacionNombre.isBlank()) {
            where.append(" AND u.nombre = ?");
            parametros.add(ubicacionNombre);
            if (esDetalle) {
                where.append(" AND da.idUbicacion IS NOT NULL");
            }
        }
    }

    private int ejecutarConteo(String sql, List<Object> parametros) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            for (Object parametro : parametros) {
                if (parametro instanceof java.sql.Date) {
                    ps.setDate(index++, (java.sql.Date) parametro);
                } else if (parametro instanceof Integer) {
                    ps.setInt(index++, (Integer) parametro);
                } else {
                    ps.setString(index++, parametro != null ? parametro.toString() : null);
                }
            }

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



    /**
     * Verifica si una presentación existe para un producto (sin lote específico)
     * Para uso en modo rápido
     */
    public boolean existePresentacionParaProducto(String idProducto, String presentacion) {
        String sql = """
        SELECT 1
        FROM (
            SELECT da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) x
        LIMIT 1
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setString(index++, idProducto);
            ps.setString(index, presentacion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un factor existe para una combinación producto-presentación (sin lote específico)
     * Para uso en modo rápido
     */
    public boolean existeFactorParaProductoPresentacion(String idProducto, String presentacion, int factor) {
        String sql = """
        SELECT 1
        FROM (
            SELECT da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) x
        LIMIT 1
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index, factor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si una combinación producto-presentación-factor existe en inventario
     * Para uso en modo rápido
     */
    public boolean existeCombinacionProductoPresentacionFactor(String idProducto, String presentacion, int factor) {
        String sql = """
        SELECT 1
        FROM (
            SELECT da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) x
        LIMIT 1
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index, factor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene los precios de la última entrada de un producto con presentación y factor específicos
     * Para uso en modo rápido
     */
    public Optional<PreciosProducto> obtenerPreciosProductoUltimaEntradaConPresentacion(
            String idProducto, String presentacion, int factor) {
        String sql = """
        SELECT de.precioUnitario, de.precioIVA, de.precioBrutoTotal, de.precioTotal
        FROM detalle_Entrada de
        JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada
        WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(agregarFiltroEstado(sql, conn)
                     + " ORDER BY de.idDetalleEntrada DESC LIMIT 1")) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
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

        // Si no encuentra con presentación y factor específicos, intentar con el método genérico
        return obtenerPreciosProductoUltimaEntrada(idProducto);
    }

    /**
     * Obtiene la cantidad disponible total de un producto con presentación y factor específicos
     * Para uso en modo rápido
     */
    public int obtenerCantidadTotalDisponibleProductoPresentacionFactor(
            String idProducto, String presentacion, int factor) {
        String sql = """
        SELECT COUNT(*) AS total FROM (
            SELECT da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) t
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index, factor);
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

    /**
     * Verifica si hay suficiente cantidad disponible para un producto con presentación y factor específicos
     * Para uso en modo rápido
     */
    public boolean verificarDisponibilidadSuficiente(String idProducto, String presentacion,
                                                     int factor, int cantidadRequerida) {
        int disponible = obtenerCantidadDisponibleProductoPresentacionFactor(idProducto, presentacion, factor);
        return disponible >= cantidadRequerida;
    }

    /**
     * Obtiene las disponibilidades por lote para un producto con presentación y factor específicos
     * Ordenado por fecha de caducidad (más cercana primero)
     * Para uso en modo rápido
     */
    public List<DisponibilidadPorLote> obtenerDisponibilidadesPorLote(String idProducto,
                                                                      String presentacion, int factor) {
        List<DisponibilidadPorLote> resultado = new ArrayList<>();
        String sql = """
        SELECT z.lote, z.caducidad, COUNT(*) AS cantidad
        FROM (
            SELECT a.lote, a.caducidad, da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT a.lote, a.caducidad, CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) z
        GROUP BY z.lote, z.caducidad
        ORDER BY 
            CASE 
                WHEN z.caducidad IS NOT NULL THEN 0
                ELSE 1
            END,
            z.caducidad ASC, 
            z.lote ASC
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index, factor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String lote = rs.getString("lote");
                    java.sql.Date caducidad = rs.getDate("caducidad");
                    int cantidad = rs.getInt("cantidad");
                    java.time.LocalDate caducidadLocal = caducidad != null ? caducidad.toLocalDate() : null;
                    resultado.add(new DisponibilidadPorLote(lote, caducidadLocal, cantidad));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    /**
     * Clase interna para representar disponibilidad por lote
     */
    public static class DisponibilidadPorLote {
        private final String lote;
        private final java.time.LocalDate caducidad;
        private final int cantidad;

        public DisponibilidadPorLote(String lote, java.time.LocalDate caducidad, int cantidad) {
            this.lote = lote != null ? lote : "";
            this.caducidad = caducidad;
            this.cantidad = cantidad;
        }

        public String getLote() {
            return lote;
        }

        public java.time.LocalDate getCaducidad() {
            return caducidad;
        }

        public int getCantidad() {
            return cantidad;
        }
    }

    /**
     * Obtiene las ubicaciones disponibles para un producto con presentación y factor específicos
     * Ordenadas por cantidad disponible (mayor a menor)
     * Para uso en modo rápido
     */
    public List<UbicacionDisponible> obtenerUbicacionesDisponibles(String idProducto,
                                                                   String presentacion, int factor) {
        List<UbicacionDisponible> resultado = new ArrayList<>();
        String sql = """
        SELECT z.ubicacion, COUNT(*) AS cantidad
        FROM (
            SELECT u.nombre AS ubicacion, da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            JOIN ubicaciones u ON u.id = da.idUbicacion
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT u.nombre AS ubicacion, CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            JOIN ubicaciones u ON u.id = a.ubicacion
            WHERE de.claveProducto = ? AND a.presentacion = ? AND a.factor = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) z
        GROUP BY z.ubicacion
        ORDER BY cantidad DESC, z.ubicacion ASC
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index++, factor);
            ps.setString(index++, idProducto);
            ps.setString(index++, presentacion);
            ps.setInt(index, factor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ubicacion = rs.getString("ubicacion");
                    int cantidad = rs.getInt("cantidad");
                    resultado.add(new UbicacionDisponible(ubicacion, cantidad));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    /**
     * Clase interna para representar ubicaciones disponibles
     */
    public static class UbicacionDisponible {
        private final String ubicacion;
        private final int cantidad;

        public UbicacionDisponible(String ubicacion, int cantidad) {
            this.ubicacion = ubicacion != null ? ubicacion : "";
            this.cantidad = cantidad;
        }

        public String getUbicacion() {
            return ubicacion;
        }

        public int getCantidad() {
            return cantidad;
        }
    }

    public boolean verificarExistenciaProducto(String idProducto, String lote) {
        String sql = """
        SELECT 1
        FROM (
            SELECT da.idDetalle AS unidad
            FROM detalleArticulo da
            JOIN articulo a ON a.idArticulo = da.idArticulo
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
              AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
            UNION ALL
            SELECT CAST(a.idArticulo AS CHAR) AS unidad
            FROM articulo a
            JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada
            WHERE de.claveProducto = ?
              AND a.lote = ?
              AND (a.idDetalleSalida IS NULL OR a.idDetalleSalida = 0)
              AND LOWER(a.estado) = 'disponible'
              AND NOT EXISTS (
                    SELECT 1
                    FROM detalleArticulo da
                    WHERE da.idArticulo = a.idArticulo
                      AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0)
                      AND LOWER(COALESCE(da.estado, '')) IN ('activo', 'disponible')
              )
        ) x
        LIMIT 1
    """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);
            ps.setString(2, lote);
            ps.setString(3, idProducto);
            ps.setString(4, lote);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
